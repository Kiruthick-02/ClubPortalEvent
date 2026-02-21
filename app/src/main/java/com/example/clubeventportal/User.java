package com.example.clubeventportal;

public class User {
    private String uid;
    private String name;
    private String email;
    private String role;

    // Empty constructor required for Firestore
    public User() {
    }

    public User(String uid, String name, String email, String role) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // --- GETTERS ---
    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getEmail() { return email; } // This fixes the previous .getEmail error
    public String getRole() { return role; }
}