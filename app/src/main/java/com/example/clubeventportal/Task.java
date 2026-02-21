package com.example.clubeventportal;

public class Task {
    private String id, description;
    private boolean completed;

    public Task() {}

    public Task(String id, String description, boolean completed) {
        this.id = id;
        this.description = description;
        this.completed = completed;
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public boolean isCompleted() { return completed; }
}