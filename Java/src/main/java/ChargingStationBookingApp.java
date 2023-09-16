import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.TextSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.MapView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ChargingStationBookingApp extends Application {

    // Database connection parameters
    private static final String DATABASE_URL = "jdbc:sqlite:charging_station.db";
    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS bookings (id INTEGER PRIMARY KEY AUTOINCREMENT, start_time TEXT, end_time TEXT, location TEXT)";
    private static final String INSERT_BOOKING_SQL = "INSERT INTO bookings (start_time, end_time, location) VALUES (?, ?)";
    private static final String SELECT_BOOKINGS_SQL = "SELECT * FROM bookings";
    private static final String DELETE_BOOKING_SQL = "DELETE FROM bookings WHERE id = ?";

    private Connection connection;
    private TableView<Booking> tableView;
    private ObservableList<Booking> bookings;
    private MapView mapView;

    private GeocodeParameters geocodeParameters;
    private GraphicsOverlay graphicsOverlay;
    private LocatorTask locatorTask;

    private TextField searchBox;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
//        ArcGISRuntimeEnvironment.setInstallDirectory("/Users/ambin04245/Downloads/arcgis-runtime-sdk-java-200.2.0");
        initializeDatabase();
        String yourApiKey = "AAPK9b1070d636d94f7886a6474a2751a9ca_MBQoO3pxkeibq99KBzD247WpsDsvjsHJn8nqhpsU1eerd_WQDGkBp_CtBhEJV0Q";
        ArcGISRuntimeEnvironment.setApiKey(yourApiKey);
        StackPane stackPane = new StackPane();

        // Create UI components
        Label titleLabel = new Label("EV Charging Station Booking System");
        Label startTimeLabel = new Label("Start Time*:");
        TextField startTimeField = new TextField();
        Label endTimeLabel = new Label("End Time*:");
        TextField endTimeField = new TextField();
//        Label locationLabel = new Label("Location*:");
        // Create a ComboBox for charging location selection
        ComboBox<String> locationComboBox = new ComboBox<>();
        locationComboBox.setPromptText("Select Charging Location*");
        locationComboBox.getItems().addAll("Location A", "Location B", "Location C"); // Add available locations
        ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC);
        // set the map on the map view
        mapView.setMap(map);
        mapView.setViewpoint(new Viewpoint(34.02700, -118.80543, 144447.638572));

        // create a graphics overlay and add it to the map view
        graphicsOverlay = new GraphicsOverlay();

        mapView.getGraphicsOverlays().add(graphicsOverlay);
        setupTextField();
        createLocatorTaskAndDefaultParameters();
        stackPane.getChildren().add(searchBox);
        StackPane.setAlignment(searchBox, Pos.TOP_LEFT);
        StackPane.setMargin(searchBox, new Insets(10, 0, 0, 10));
        Button bookButton = new Button("Book Slot");


        Button viewButton = new Button("View Bookings");
        Button cancelButton = new Button("Cancel Booking");

        tableView = new TableView<>();
        tableView.setEditable(false);

        TableColumn<Booking, String> startTimeCol = new TableColumn<>("Start Time");
        startTimeCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        TableColumn<Booking, String> endTimeCol = new TableColumn<>("End Time");
        endTimeCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        tableView.getColumns().addAll(startTimeCol, endTimeCol);
        bookings = FXCollections.observableArrayList();
        tableView.setItems(bookings);

        // Event handlers
        bookButton.setOnAction(e -> bookSlot(startTimeField.getText(), endTimeField.getText(), locationComboBox.getValue()));
        viewButton.setOnAction(e -> viewBookings());
        cancelButton.setOnAction(e -> cancelBooking());

        // Create layout
        VBox vbox = new VBox(10);
        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(
                titleLabel,
                startTimeLabel,
                startTimeField,
                endTimeLabel,
                endTimeField,
                new Label("Charging Location:"), // Label for the ComboBox
                locationComboBox, // Add the ComboBox for charging location
                bookButton,
                viewButton,
                tableView,
                cancelButton
        );

        // Create the scene and set it on the stage
        Scene scene = new Scene(vbox, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("EV Charging Station Booking System");
        primaryStage.show();
    }

    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DATABASE_URL);
            Statement statement = connection.createStatement();
            statement.execute(CREATE_TABLE_SQL);
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void bookSlot(String startTime, String endTime, String location) {
        // Check if any of the mandatory fields are empty
        if (startTime.isEmpty() || endTime.isEmpty() || location == null) {
            System.out.println("Error: Please fill in all mandatory fields.");
            return; // Prevent booking if any field is empty
        }
        if (!isOverlapping(startTime, endTime)) {
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(INSERT_BOOKING_SQL);
                preparedStatement.setString(1, startTime);
                preparedStatement.setString(2, endTime);
                preparedStatement.setString(3, location); // Add location to the statement
                preparedStatement.executeUpdate();
                preparedStatement.close();
                viewBookings(); // Refresh the booking list after a successful booking
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {// Check for overlapping bookings
            if (!isOverlapping(startTime, endTime)) {
                try {
                    PreparedStatement preparedStatement = connection.prepareStatement(INSERT_BOOKING_SQL);
                    preparedStatement.setString(1, startTime);
                    preparedStatement.setString(2, endTime);
                    preparedStatement.executeUpdate();
                    preparedStatement.close();
                    viewBookings(); // Refresh the booking list after a successful booking
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                // Handle overlapping booking error
                System.out.println("Error: Overlapping booking detected.");
            }
        }
    }

    private boolean isOverlapping(String newStartTime, String newEndTime) {
        // Check if the new booking overlaps with any existing bookings
        for (Booking existingBooking : bookings) {
            String existingStartTime = existingBooking.getStartTime();
            String existingEndTime = existingBooking.getEndTime();

            // Parse time strings into integers for comparison
            int newStart = Integer.parseInt(newStartTime.replace(":", ""));
            int newEnd = Integer.parseInt(newEndTime.replace(":", ""));
            int existingStart = Integer.parseInt(existingStartTime.replace(":", ""));
            int existingEnd = Integer.parseInt(existingEndTime.replace(":", ""));

            // Check for overlap
            if ((newStart >= existingStart && newStart < existingEnd) ||
                    (newEnd > existingStart && newEnd <= existingEnd) ||
                    (newStart <= existingStart && newEnd >= existingEnd)) {
                return true; // Overlapping booking detected
            }
        }
        return false; // No overlapping booking detected
    }


    private void viewBookings() {
        bookings.clear();
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(SELECT_BOOKINGS_SQL);
            while (resultSet.next()) {
                String startTime = resultSet.getString("start_time");
                String endTime = resultSet.getString("end_time");
                bookings.add(new Booking(startTime, endTime));
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void cancelBooking() {
        Booking selectedBooking = tableView.getSelectionModel().getSelectedItem();
        if (selectedBooking != null) {
            int bookingId = selectedBooking.getId();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(DELETE_BOOKING_SQL);
                preparedStatement.setInt(1, bookingId);
                preparedStatement.executeUpdate();
                preparedStatement.close();

                // Remove the canceled booking from the observable list
                bookings.remove(selectedBooking);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupTextField() {
        searchBox = new TextField();
        searchBox.setMaxWidth(400);
        searchBox.setPromptText("Search for an address");
    }
    private void createLocatorTaskAndDefaultParameters() {
        locatorTask = new LocatorTask("https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer");

        geocodeParameters = new GeocodeParameters();
        geocodeParameters.getResultAttributeNames().add("*");
        geocodeParameters.setMaxResults(1);
        geocodeParameters.setOutputSpatialReference(mapView.getSpatialReference());
    }
    private void performGeocode(String address) {
        ListenableFuture<List<GeocodeResult>> geocodeResults = locatorTask.geocodeAsync(address, geocodeParameters);

        geocodeResults.addDoneListener(() -> {
            try {
                List<GeocodeResult> geocodes = geocodeResults.get();
                if (geocodes.size() > 0) {
                    GeocodeResult result = geocodes.get(0);

                } else {
                    new Alert(Alert.AlertType.INFORMATION, "No results found.").show();
                }
            } catch (InterruptedException | ExecutionException e) {
                new Alert(Alert.AlertType.ERROR, "Error getting result.").show();
                e.printStackTrace();
            }
        });
    }

    @Override
    public void stop() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class Booking {
        private int id;
        private String startTime;
        private String endTime;

        private String location;

        public Booking(String startTime, String endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.location = location;
        }
        // Getters and setters for the location field
        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
    }
}
