package com.segi.student.blog.ui.my;

import com.google.firebase.database.Exclude;

// This class represents a user in a list (Following/Followers)
public class UserItem {
    @Exclude
    private String uid;
    private String displayName;
    private String avatarUrl;
    private String bio;

    // Firebase requires a public no-argument constructor
    public UserItem() {}

    public UserItem(String uid, String displayName, String avatarUrl, String bio) {
        this.uid = uid;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
    }

    // --- Getters and Setters ---
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
