package com.example.clubeventportal;

import java.io.Serializable;

public class ClubEvent implements Serializable {
    private String eventId, title, description, date, posterUrl, clubName, clubId;
    private String venue, time;
    private int registrationLimit;
    private double entryFee;

    // NEW FIELD: Duration in minutes (e.g., 60 mins)
    private int attendanceDuration;

    private double budget;
    private long timestamp;
    private long views, interested, notInterested;

    public ClubEvent() {}

    public ClubEvent(String eventId, String title, String description, String date, String time, String venue, int registrationLimit, double entryFee, int attendanceDuration, String posterUrl, String clubName, String clubId, double budget) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = time;
        this.venue = venue;
        this.registrationLimit = registrationLimit;
        this.entryFee = entryFee;
        this.attendanceDuration = attendanceDuration; // Set new field
        this.posterUrl = posterUrl;
        this.clubName = clubName;
        this.clubId = clubId;
        this.budget = budget;
        this.timestamp = System.currentTimeMillis();
        this.views = 0;
        this.interested = 0;
        this.notInterested = 0;
    }

    // Getters
    public String getEventId() { return eventId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getVenue() { return venue; }
    public int getRegistrationLimit() { return registrationLimit; }
    public double getEntryFee() { return entryFee; }
    public int getAttendanceDuration() { return attendanceDuration; } // New Getter
    public String getPosterUrl() { return posterUrl; }
    public String getClubName() { return clubName; }
    public String getClubId() { return clubId; }
    public double getBudget() { return budget; }
    public long getTimestamp() { return timestamp; }
    public long getViews() { return views; }
    public long getInterested() { return interested; }
    public long getNotInterested() { return notInterested; }
}