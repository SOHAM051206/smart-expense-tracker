package com.soham;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ExpenseTrackerApp extends Application {

    private double income = 0;
    private final ObservableList<Expense> expenses =
            FXCollections.observableArrayList();

    private Label totalLabel = new Label("Total: Rs 0");
    private Label balanceLabel = new Label("Balance: Rs 0");

    private static final String FILE_NAME = "expenses.csv";

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void start(Stage stage) {

        loadExpenses();

        // ===== SET APP LOGO =====
        try {
            Image icon = new Image(getClass().getResourceAsStream("/logo.png"));
            stage.getIcons().add(icon);
        } catch (Exception ignored) {}

        // ===== TOP BAR =====
        Label incomeLabel = new Label("Monthly Income:");
        TextField incomeField = new TextField();
        incomeField.setPrefWidth(140);
        incomeField.setPromptText("Income");

        Button setIncomeBtn = new Button("Set Income");

        ComboBox<String> themeSelector = new ComboBox<>();
        themeSelector.getItems().addAll("Light", "Dark", "System");
        themeSelector.setValue("System");

        HBox topBar = new HBox(15,
                incomeLabel,
                incomeField,
                setIncomeBtn,
                new Label("Theme:"),
                themeSelector
        );
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10));

        // ===== EXPENSE SECTION =====
        Label expenseLabel = new Label("Add Expense");

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");
        amountField.setPrefWidth(140);

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Food", "Travel", "Shopping", "Bills", "Custom");
        categoryBox.setPrefWidth(160);
        categoryBox.setPromptText("Category");

        TextField customCategoryField = new TextField();
        customCategoryField.setPromptText("Custom category");
        customCategoryField.setPrefWidth(160);
        customCategoryField.setManaged(false);
        customCategoryField.setVisible(false);

        categoryBox.setOnAction(e -> {
            boolean isCustom = "Custom".equals(categoryBox.getValue());
            customCategoryField.setManaged(isCustom);
            customCategoryField.setVisible(isCustom);
        });

        // ===== DATE PICKER WITH dd/MM/yyyy FORMAT =====
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(160);

        datePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return (date != null) ? FORMATTER.format(date) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                return (string != null && !string.isEmpty())
                        ? LocalDate.parse(string, FORMATTER)
                        : null;
            }
        });

        Button addBtn = new Button("Add Expense");

        HBox expenseBox = new HBox(12,
                amountField,
                categoryBox,
                customCategoryField,
                datePicker,
                addBtn
        );
        expenseBox.setAlignment(Pos.CENTER_LEFT);
        expenseBox.setPadding(new Insets(10));

        // ===== TABLE =====
        TableView<Expense> table = new TableView<>(expenses);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(420);

        TableColumn<Expense, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleDoubleProperty(
                        data.getValue().getAmount()).asObject());

        TableColumn<Expense, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getCategory()));

        TableColumn<Expense, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        data.getValue().getDate()));

        // Display formatted date in table
        dateCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? "" : FORMATTER.format(date));
            }
        });

        table.getColumns().addAll(amountCol, categoryCol, dateCol);

        Button deleteBtn = new Button("Delete Selected");

        // ===== SUMMARY =====
        HBox summaryBox = new HBox(50, totalLabel, balanceLabel);
        summaryBox.setPadding(new Insets(15));
        summaryBox.setAlignment(Pos.CENTER_LEFT);

        // ===== ACTIONS =====
        setIncomeBtn.setOnAction(e -> {
            try {
                income = Double.parseDouble(incomeField.getText());
                updateTotals();
            } catch (Exception ex) {
                showError("Enter valid income.");
            }
        });

        addBtn.setOnAction(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText());
                String category = categoryBox.getValue();

                if (category == null) {
                    showError("Select category.");
                    return;
                }

                if ("Custom".equals(category)) {
                    category = customCategoryField.getText();
                    if (category.isEmpty()) {
                        showError("Enter custom category.");
                        return;
                    }
                }

                Expense expense = new Expense(amount, category, datePicker.getValue());
                expenses.add(expense);
                saveExpenses();
                updateTotals();

                amountField.clear();
                customCategoryField.clear();
                categoryBox.getSelectionModel().clearSelection();

            } catch (Exception ex) {
                showError("Enter valid amount.");
            }
        });

        deleteBtn.setOnAction(e -> {
            Expense selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                expenses.remove(selected);
                saveExpenses();
                updateTotals();
            }
        });

        VBox root = new VBox(18,
                topBar,
                expenseLabel,
                expenseBox,
                table,
                deleteBtn,
                summaryBox
        );

        root.setPadding(new Insets(25));

        Scene scene = new Scene(root, 1050, 720);

        applyTheme(scene, "System");

        themeSelector.setOnAction(e ->
                animateThemeChange(scene, themeSelector.getValue())
        );

        updateTotals();

        stage.setTitle("Smart Expense Tracker");
        stage.setScene(scene);
        stage.show();
    }

    // ===== THEME ANIMATION =====
    private void animateThemeChange(Scene scene, String theme) {

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), scene.getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.4);

        fadeOut.setOnFinished(e -> {
            applyTheme(scene, theme);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), scene.getRoot());
            fadeIn.setFromValue(0.4);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });

        fadeOut.play();
    }

    private void applyTheme(Scene scene, String theme) {
        scene.getStylesheets().clear();

        if ("Light".equals(theme)) {
            scene.getStylesheets().add(
                    getClass().getResource("/light.css").toExternalForm()
            );
        } else if ("Dark".equals(theme)) {
            scene.getStylesheets().add(
                    getClass().getResource("/dark.css").toExternalForm()
            );
        }
    }

    private void updateTotals() {
        double total = expenses.stream()
                .mapToDouble(Expense::getAmount)
                .sum();

        totalLabel.setText("Total: Rs " + total);
        balanceLabel.setText("Balance: Rs " + (income - total));
    }

    private void saveExpenses() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Expense e : expenses) {
                writer.println(e.getAmount() + "," +
                        e.getCategory() + "," +
                        e.getDate());
            }
        } catch (IOException ignored) {}
    }

    private void loadExpenses() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                expenses.add(new Expense(
                        Double.parseDouble(parts[0]),
                        parts[1],
                        LocalDate.parse(parts[2])
                ));
            }
        } catch (IOException ignored) {}
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(msg);
        alert.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
