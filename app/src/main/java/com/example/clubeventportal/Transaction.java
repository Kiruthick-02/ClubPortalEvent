package com.example.clubeventportal;

public class Transaction {
    private String id;
    private String type; // "INCOME" or "EXPENSE"
    private String category; // e.g., "Sponsorship", "Food", "Venue"
    private double amount;
    private String status; // "PENDING", "APPROVED", "REJECTED"
    private long timestamp;
    private String addedBy; // User ID

    public Transaction() {}

    public Transaction(String id, String type, String category, double amount, String status, String addedBy) {
        this.id = id;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.status = status;
        this.addedBy = addedBy;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
    public String getAddedBy() { return addedBy; }
}