package com.example.clubeventportal;

public class Registration {
    private String userId, name, regNo, dept, phone, email;
    private long timestamp;
    private boolean attended;

    public Registration() {}

    public Registration(String userId, String name, String regNo, String dept, String phone, String email) {
        this.userId = userId;
        this.name = name;
        this.regNo = regNo;
        this.dept = dept;
        this.phone = phone;
        this.email = email;
        this.timestamp = System.currentTimeMillis();
        this.attended = false; // Default false
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getRegNo() { return regNo; }
    public String getDept() { return dept; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public boolean isAttended() { return attended; }
    public long getTimestamp() { return timestamp; }
}