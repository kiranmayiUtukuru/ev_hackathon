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

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        initializeDatabase();

        // Create UI components
        Label titleLabel = new Label("EV Charging Station Booking System");
        Label startTimeLabel = new Label("Start Time:");
        TextField startTimeField = new TextField();
        Label endTimeLabel = new Label("End Time:");
        TextField endTimeField = new TextField();
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
        bookButton.setOnAction(e -> bookSlot(startTimeField.getText(), endTimeField.getText()));
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

    private void bookSlot(String startTime, String endTime) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(INSERT_BOOKING_SQL);
            preparedStatement.setString(1, startTime);
            preparedStatement.setString(2, endTime);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
                viewBookings(); // Refresh the booking list after cancellation
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
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
