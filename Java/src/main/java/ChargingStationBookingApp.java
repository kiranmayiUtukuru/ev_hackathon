import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.*;
import com.esri.arcgisruntime.mapping.view.WrapAroundMode;
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
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;

import com.esri.arcgisruntime.mapping.view.MapView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;


public class ChargingStationBookingApp extends Application {

    // Database connection parameters
    private static final String DATABASE_URL = "jdbc:sqlite:charging_station.db";
    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS bookings (id INTEGER PRIMARY KEY AUTOINCREMENT, start_time TEXT, end_time TEXT)";
    private static final String INSERT_BOOKING_SQL = "INSERT INTO bookings (start_time, end_time) VALUES (?, ?)";
    private static final String SELECT_BOOKINGS_SQL = "SELECT * FROM bookings";
    private static final String DELETE_BOOKING_SQL = "DELETE FROM bookings WHERE id = ?";

    private Connection connection;
    private TableView<Booking> tableView;
    private ObservableList<Booking> bookings;
    private TextField searchBox;
    private MapView mapView;

    public static void main(String[] args) {
        launch(args);
        String yourApiKey = "AAPK9b1070d636d94f7886a6474a2751a9ca_MBQoO3pxkeibq99KBzD247WpsDsvjsHJn8nqhpsU1eerd_WQDGkBp_CtBhEJV0Q";
        ArcGISRuntimeEnvironment.setApiKey(yourApiKey);
    }

    @Override
    public void start(Stage stage) {

        initializeDatabase();
        String yourApiKey = "AAPK9b1070d636d94f7886a6474a2751a9ca_MBQoO3pxkeibq99KBzD247WpsDsvjsHJn8nqhpsU1eerd_WQDGkBp_CtBhEJV0Q";
        ArcGISRuntimeEnvironment.setApiKey(yourApiKey);

        // Create UI components
        Label titleLabel = new Label("EV Charging Station Booking System");
        Label startTimeLabel = new Label("Start Time*:");
        TextField startTimeField = new TextField();
        Label endTimeLabel = new Label("End Time*:");
        TextField endTimeField = new TextField();

        // set the title and size of the stage and show it
        stage.setTitle("Display a map tutorial");
        stage.setWidth(800);
        stage.setHeight(700);
        stage.show();

// create a JavaFX scene with a stack pane as the root node, and add it to the scene
        StackPane stackPane = new StackPane();
        Scene scene = new Scene(stackPane);
        stage.setScene(scene);
        // create a map view to display the map and add it to the stack pane
        mapView = new MapView();
        stackPane.getChildren().add(mapView);

        ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC);

        // set the map on the map view
        mapView.setMap(map);

        mapView.setViewpoint(new Viewpoint(34.02700, -118.80543, 144447.638572));
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
        // Set the initial viewpoint (center and scale) of the map
        Viewpoint initialViewpoint = new Viewpoint(34.02700, -118.80543, 144447.638572);
        mapView.setViewpoint(initialViewpoint);
        // Specify the desired width and height for the MapView
        mapView.setPrefSize(200, 200); // Replace with your desired width and height


// Set the wrap around mode for the map (optional)
        mapView.setWrapAroundMode(WrapAroundMode.ENABLE_WHEN_SUPPORTED);

// Create an ArcGIS Online basemap and set it to the map
//        Basemap basemap = new Basemap();
//        map.setBasemap(basemap);

// Optionally, add additional layers to the map (e.g., feature layers)
        // Replace "serviceUrl" with the actual URL of your feature service
        String serviceUrl = " ";

// Create a ServiceFeatureTable using the service URL
        ServiceFeatureTable featureTable = new ServiceFeatureTable(serviceUrl);

// Load the feature table (optional but recommended)
        featureTable.loadAsync();

// Create a FeatureLayer using the feature table
        FeatureLayer featureLayer = new FeatureLayer(featureTable);

//        // Check if the featureLayer is not already added to the map
//        if (!map.getOperationalLayers().contains(featureLayer)) {
//            // Add the feature layer to your map
//            map.getOperationalLayers().add(featureLayer);
//        }

        LayerList layers = map.getOperationalLayers();
        layers.add(featureLayer);

        bookings = FXCollections.observableArrayList();
        tableView.setItems(bookings);

        // Event handlers
        bookButton.setOnAction(e -> bookSlot(startTimeField.getText(), endTimeField.getText()));
        viewButton.setOnAction(e -> viewBookings());
        cancelButton.setOnAction(e -> cancelBooking());
        // Create a Label with the desired text
        Label mapViewLabel = new Label("EV Charging locations nearby you");
        mapViewLabel.setStyle("-fx-font-size: 16px;"); // Optional: Set the font size

// Create a VBox to hold the Label and the MapView
        VBox vboxMap = new VBox(10); // You can specify spacing between elements
        vboxMap.setAlignment(Pos.CENTER);

// Add the Label to the VBox
        vboxMap.getChildren().add(mapViewLabel);

// Set the preferred size for the MapView
        mapView.setPrefSize(400, 400); // Replace with your desired width and height

// Add the MapView to the VBox
        vboxMap.getChildren().add(mapView);

// Create other UI elements and add them to the VBox as needed

// Create the scene and set it on the stage
        Scene sceneMap = new Scene(vboxMap, 1000, 1000); // Replace with your desired scene size
        stage.setScene(sceneMap);

        // Create layout
        VBox vbox = new VBox(10);
        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(
                titleLabel,
                startTimeLabel,
                startTimeField,
                endTimeLabel,
                endTimeField,
                mapViewLabel,
                mapView,
                bookButton,
                viewButton,
                tableView,
                cancelButton

        );

        // Create the scene and set it on the stage
        scene = new Scene(vbox, 2400, 2400); // Set a size for the scene
        stage.setScene(scene);
        stage.setTitle("EV Charging Station Booking System");
        stage.show();
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

    private void bookSlot(String startTime, String endTime) {
        // Check if any of the mandatory fields are empty
        if (startTime.isEmpty() || endTime.isEmpty() ) {
            System.out.println("Error: Please fill in all mandatory fields.");
            return; // Prevent booking if any field is empty
        }

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

    @Override
    public void stop() {
        if (mapView != null) {
            mapView.dispose();
        }

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
