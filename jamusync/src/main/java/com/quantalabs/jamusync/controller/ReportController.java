package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.dao.TransactionDAO;
import com.quantalabs.jamusync.dao.TransactionItemDAO;
import com.quantalabs.jamusync.model.Transaction;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ReportController {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Label revenueLabel;
    @FXML private Label expensesLabel;
    @FXML private Label profitLabel;
    @FXML private Label transactionCountLabel;
    @FXML private Label messageLabel;

    // The three charts (populated when the user clicks Generate Report).
    @FXML private PieChart pieChart;
    @FXML private BarChart<String, Number> barChart;
    @FXML private LineChart<String, Number> lineChart;

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final TransactionItemDAO transactionItemDAO = new TransactionItemDAO();
    private final DecimalFormat formatter = new DecimalFormat("Rp #,###");

    @FXML
    public void initialize() {
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.withDayOfMonth(1));
        endDatePicker.setValue(today);
        handleGenerate(null);
    }

    @FXML
    public void handleGenerate(ActionEvent event) {
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            showError("Please select both start and end dates.");
            return;
        }
        if (startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
            showError("Start date cannot be after end date.");
            return;
        }

        String start = startDatePicker.getValue().toString();
        String end = endDatePicker.getValue().toString();

        double revenue = transactionDAO.getRevenueByDateRange(start, end);
        double expenses = transactionItemDAO.getTotalCostByDateRange(start, end);
        double profit = revenue - expenses;

        List<Transaction> completed = transactionDAO.getFilteredTransactions("Completed", start, end);

        revenueLabel.setText(formatter.format(revenue));
        expensesLabel.setText(formatter.format(expenses));
        profitLabel.setText(formatter.format(profit));
        profitLabel.getStyleClass().removeAll("metric-positive", "metric-warning");
        if (profit >= 0) {
            profitLabel.getStyleClass().add("metric-positive");
        } else {
            profitLabel.getStyleClass().add("metric-warning");
        }
        transactionCountLabel.setText(completed.size() + " completed transaction(s)");

        // Only draw the charts when the user actually clicks Generate Report
        // (event is null when we auto-run once on screen load).
        if (event != null) {
            populateCharts(start, end);
        }

        showSuccess("Report generated for " + start + " to " + end + ".");
    }

    // Fill the three charts with data for the chosen date range.
    private void populateCharts(String start, String end) {
        // A) PIE CHART - best selling products (top 5 by quantity).
        pieChart.getData().clear();
        List<Map.Entry<String, Integer>> sales = transactionItemDAO.getSalesByProduct(start, end);
        for (Map.Entry<String, Integer> entry : sales) {
            pieChart.getData().add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
        }

        // B) BAR CHART - monthly revenue and cost.
        barChart.getData().clear();
        XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
        revenueSeries.setName("Revenue");
        XYChart.Series<String, Number> costSeries = new XYChart.Series<>();
        costSeries.setName("Cost");

        List<TransactionDAO.MonthlyRevenueCost> monthly = transactionDAO.getMonthlyRevenueCost(start, end);
        for (TransactionDAO.MonthlyRevenueCost m : monthly) {
            revenueSeries.getData().add(new XYChart.Data<>(m.month, m.revenue));
            costSeries.getData().add(new XYChart.Data<>(m.month, m.cost));
        }
        barChart.getData().add(revenueSeries);
        barChart.getData().add(costSeries);

        // Color the bars: green revenue, brown cost.
        for (XYChart.Data<String, Number> data : revenueSeries.getData()) {
            if (data.getNode() != null) {
                data.getNode().setStyle("-fx-bar-fill: #5C7A4F;");
            }
        }
        for (XYChart.Data<String, Number> data : costSeries.getData()) {
            if (data.getNode() != null) {
                data.getNode().setStyle("-fx-bar-fill: #8B5A3C;");
            }
        }

        // C) LINE CHART - revenue trend by day.
        lineChart.getData().clear();
        XYChart.Series<String, Number> trendSeries = new XYChart.Series<>();
        trendSeries.setName("Revenue");
        List<Map.Entry<String, Double>> daily = transactionDAO.getDailyRevenue(start, end);
        for (Map.Entry<String, Double> entry : daily) {
            trendSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        lineChart.getData().add(trendSeries);

        // Color the trend line sage green.
        if (trendSeries.getNode() != null) {
            trendSeries.getNode().setStyle("-fx-stroke: #5C7A4F;");
        }
    }

    @FXML
    public void handleBack(ActionEvent event) {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/OwnerDashboard.fxml", "JamuSync - Owner Dashboard");
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("label-success");
        if (!messageLabel.getStyleClass().contains("label-error")) {
            messageLabel.getStyleClass().add("label-error");
        }
    }

    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("label-error");
        if (!messageLabel.getStyleClass().contains("label-success")) {
            messageLabel.getStyleClass().add("label-success");
        }
    }
}
