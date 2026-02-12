package com.soham;

import java.time.LocalDate;

public class Expense {

    private final double amount;
    private final String category;
    private final LocalDate date;

    public Expense(double amount, String category, LocalDate date) {
        this.amount = amount;
        this.category = category;
        this.date = date;
    }

    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public LocalDate getDate() { return date; }
}
