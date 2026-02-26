package com.example.forestclient;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.*;

public class ForestApp extends Application {

    private Connection connection;

    private TextField plotNameField = new TextField();
    private TextField locationField = new TextField();
    private DatePicker surveyDatePicker = new DatePicker();
    private TextArea notesArea = new TextArea();
    private ListView<String> plotList = new ListView<>();

    private Button saveBtn = new Button("Сохранить");
    private Button cancelBtn = new Button("Отменить");
    private int selectedId = -1;

    private TabPane tabPane;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        connectDatabase();

        tabPane = new TabPane();

        Tab formTab = new Tab("Управление записями о участках", createFormPane());
        formTab.setClosable(false);

        Tab listTab = new Tab("Список участков", createListPane());
        listTab.setClosable(false);

        tabPane.getTabs().addAll(formTab, listTab);

        Scene scene = new Scene(tabPane, 900, 400);
        primaryStage.setTitle("Информационно-справочная система лесного хозяйства");
        primaryStage.setScene(scene);
        primaryStage.show();

        loadPlots();

        plotList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                viewSelected();
            }
        });
    }

    private VBox createFormPane() {
        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(10);
        inputGrid.setVgap(10);
        inputGrid.setPadding(new Insets(10));

        inputGrid.add(new Label("Название участка:"), 0, 0);
        inputGrid.add(plotNameField, 1, 0);
        inputGrid.add(new Label("Местоположение:"), 0, 1);
        inputGrid.add(locationField, 1, 1);
        inputGrid.add(new Label("Дата обследования:"), 0, 2);
        inputGrid.add(surveyDatePicker, 1, 2);
        inputGrid.add(new Label("Заметки:"), 0, 3);
        notesArea.setPrefHeight(100);
        inputGrid.add(notesArea, 1, 3);

        Button addBtn = new Button("Добавить участок");
        addBtn.setOnAction(e -> {
            addPlot();
            saveBtn.setDisable(true);
            cancelBtn.setDisable(true);
        });

        saveBtn.setDisable(true);
        cancelBtn.setDisable(true);
        saveBtn.setOnAction(e -> updatePlot());
        cancelBtn.setOnAction(e -> {
            clearFields();
            saveBtn.setDisable(true);
            cancelBtn.setDisable(true);
        });

        // Основные кнопки
        HBox mainButtons = new HBox(10, addBtn, saveBtn, cancelBtn);
        mainButtons.setPadding(new Insets(10));

        // Кнопка "Об авторе" внизу справа
        Button aboutBtn = new Button("Об авторе");
        aboutBtn.setOnAction(e -> showAboutPopup());
        HBox bottomBox = new HBox(aboutBtn);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setAlignment(javafx.geometry.Pos.BOTTOM_RIGHT);

        VBox formPane = new VBox(15, inputGrid, mainButtons, bottomBox);
        formPane.setPadding(new Insets(20));

        return formPane;
    }

    private VBox createListPane() {
        Button viewBtn = new Button("Просмотр");
        Button editBtn = new Button("Редактировать");
        Button deleteBtn = new Button("Удалить");
        Button assignBtn = new Button("Назначить лесника");
        Button showAssignmentsBtn = new Button("Показать лесников");

        viewBtn.setOnAction(e -> viewSelected());
        editBtn.setOnAction(e -> {
            editSelected();
            saveBtn.setDisable(false);
            cancelBtn.setDisable(false);
            tabPane.getSelectionModel().select(0);
        });
        deleteBtn.setOnAction(e -> deleteSelected());
        assignBtn.setOnAction(e -> assignRangerPopup());
        showAssignmentsBtn.setOnAction(e -> showAssignmentsPopup());

        HBox btnBox = new HBox(10, viewBtn, editBtn, deleteBtn, assignBtn, showAssignmentsBtn);
        btnBox.setPadding(new Insets(10));

        VBox listPane = new VBox(10, plotList, btnBox);
        listPane.setPadding(new Insets(10));
        return listPane;
    }

    private void connectDatabase() {
        String url = "jdbc:mysql://localhost:3306/forest_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        String user = "forest_user";
        String pass = "forest12345";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, pass);
            System.out.println("Подключение к базе успешно!");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void addPlot() {
        String sql = "INSERT INTO forest_plots (name, location, survey_date, notes) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, plotNameField.getText());
            stmt.setString(2, locationField.getText());
            stmt.setDate(3, surveyDatePicker.getValue() != null ? Date.valueOf(surveyDatePicker.getValue()) : null);
            stmt.setString(4, notesArea.getText());
            stmt.executeUpdate();
            clearFields();
            loadPlots();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadPlots() {
        plotList.getItems().clear();
        String sql = "SELECT * FROM forest_plots ORDER BY survey_date DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String item = rs.getInt("id") + ": " + rs.getString("name") +
                        " | " + rs.getString("location") +
                        " | " + rs.getDate("survey_date");
                plotList.getItems().add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void editSelected() {
        String selected = plotList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        selectedId = Integer.parseInt(selected.split(":")[0]);

        String sql = "SELECT * FROM forest_plots WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, selectedId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                plotNameField.setText(rs.getString("name"));
                locationField.setText(rs.getString("location"));
                surveyDatePicker.setValue(rs.getDate("survey_date") != null ? rs.getDate("survey_date").toLocalDate() : null);
                notesArea.setText(rs.getString("notes"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        saveBtn.setDisable(false);
        cancelBtn.setDisable(false);
        tabPane.getSelectionModel().select(0);
    }

    private void updatePlot() {
        if (selectedId == -1) return;
        String sql = "UPDATE forest_plots SET name=?, location=?, survey_date=?, notes=? WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, plotNameField.getText());
            stmt.setString(2, locationField.getText());
            stmt.setDate(3, surveyDatePicker.getValue() != null ? Date.valueOf(surveyDatePicker.getValue()) : null);
            stmt.setString(4, notesArea.getText());
            stmt.setInt(5, selectedId);
            stmt.executeUpdate();
            selectedId = -1;
            clearFields();
            saveBtn.setDisable(true);
            cancelBtn.setDisable(true);
            loadPlots();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteSelected() {
        String selected = plotList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        int id = Integer.parseInt(selected.split(":")[0]);
        String sql = "DELETE FROM forest_plots WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            loadPlots();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void clearFields() {
        plotNameField.clear();
        locationField.clear();
        surveyDatePicker.setValue(null);
        notesArea.clear();
        selectedId = -1;
    }

    private void viewSelected() {
        String selected = plotList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        int id = Integer.parseInt(selected.split(":")[0]);

        String sql = "SELECT * FROM forest_plots WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Stage viewStage = new Stage();
                viewStage.initModality(Modality.APPLICATION_MODAL);
                viewStage.setTitle("Детали участка");

                Label name = new Label("Участок: " + rs.getString("name"));
                Label loc = new Label("Местоположение: " + rs.getString("location"));
                Label survey = new Label("Дата обследования: " + rs.getDate("survey_date"));
                TextArea notes = new TextArea(rs.getString("notes"));
                notes.setEditable(false);
                notes.setWrapText(true);

                Button editBtn = new Button("Редактировать участок");
                editBtn.setOnAction(e -> {
                    selectedId = id;
                    editSelected();
                    viewStage.close();
                });

                VBox layout = new VBox(10, name, loc, survey, notes, editBtn);
                layout.setPadding(new Insets(15));

                Scene scene = new Scene(layout, 400, 300);
                viewStage.setScene(scene);
                viewStage.showAndWait();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------- Об авторе ----------
    private void showAboutPopup() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Об авторе");

        Label info = new Label("Автор: Сызин Денис \nВерсия: 1.0\nСистема: Лесное хозяйство");

        VBox layout = new VBox(10, info);
        layout.setPadding(new Insets(15));
        Scene scene = new Scene(layout, 300, 150);
        popup.setScene(scene);
        popup.showAndWait();
    }

    // ---------- Назначение лесников ----------
    private void assignRangerPopup() {
        String selected = plotList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        int plotId = Integer.parseInt(selected.split(":")[0]);

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Назначить лесника");

        TextField nameField = new TextField();

        Button assignBtn = new Button("Назначить");
        assignBtn.setOnAction(e -> {
            assignRanger(plotId, nameField.getText());
            popup.close();
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.add(new Label("Имя лесника:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(assignBtn, 1, 1);

        Scene scene = new Scene(grid, 300, 120);
        popup.setScene(scene);
        popup.showAndWait();
    }

    private void assignRanger(int plotId, String name) {
        try {
            String sql = "INSERT IGNORE INTO rangers (name) VALUES (?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.executeUpdate();
            }

            String idSql = "SELECT id FROM rangers WHERE name=?";
            int rangerId = -1;
            try (PreparedStatement stmt = connection.prepareStatement(idSql)) {
                stmt.setString(1, name);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) rangerId = rs.getInt("id");
            }

            if (rangerId == -1) return;

            String regSql = "INSERT IGNORE INTO assignments (ranger_id, plot_id) VALUES (?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(regSql)) {
                stmt.setInt(1, rangerId);
                stmt.setInt(2, plotId);
                stmt.executeUpdate();
                System.out.println(name + " назначен на участок!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAssignmentsPopup() {
        String selected = plotList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        int plotId = Integer.parseInt(selected.split(":")[0]);

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Назначенные лесники");

        ListView<String> rangerList = new ListView<>();

        String sql = "SELECT r.name, a.assignment_date " +
                "FROM assignments a JOIN rangers r ON a.ranger_id = r.id " +
                "WHERE a.plot_id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, plotId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String item = rs.getString("name") + " | " + rs.getTimestamp("assignment_date");
                rangerList.getItems().add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        VBox layout = new VBox(10, rangerList);
        layout.setPadding(new Insets(10));
        Scene scene = new Scene(layout, 400, 300);
        popup.setScene(scene);
        popup.showAndWait();
    }
}
