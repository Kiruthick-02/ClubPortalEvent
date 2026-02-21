package com.example.clubeventportal;

public class Club {
    private String clubId;
    private String clubName;
    private String description;
    private String adminId;
    private String profileImage; // New Field (Base64)

    public Club() {}

    public Club(String clubId, String clubName, String description, String adminId, String profileImage) {
        this.clubId = clubId;
        this.clubName = clubName;
        this.description = description;
        this.adminId = adminId;
        this.profileImage = profileImage;
    }

    public String getClubId() { return clubId; }
    public String getClubName() { return clubName; }
    public String getDescription() { return description; }
    public String getAdminId() { return adminId; }
    public String getProfileImage() { return profileImage; }
}