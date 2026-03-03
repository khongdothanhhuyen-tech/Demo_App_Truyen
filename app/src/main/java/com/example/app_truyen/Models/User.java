package com.example.app_truyen.Models;

import com.google.firebase.Timestamp;

public class User {

    private String uid;
    private String avatarUrl;
    private String email;
    private String role;
    private Boolean isBanned;
    private long exp;
    private Timestamp ngayTao;

    private String lastCheckIn;
    private String lastCommented;
    private String lastLiked;
    private String lastPosted;

    private int streak;

    private boolean isSelected;


    public User() {}

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getIsBanned() { return isBanned; }
    public void setIsBanned(Boolean banned) { isBanned = banned; }

    public long getExp() { return exp; }
    public void setExp(long exp) { this.exp = exp; }

    public Timestamp getNgayTao() { return ngayTao; }
    public void setNgayTao(Timestamp ngayTao) { this.ngayTao = ngayTao; }

    public String getLastCheckIn() { return lastCheckIn; }
    public void setLastCheckIn(String lastCheckIn) { this.lastCheckIn = lastCheckIn; }

    public String getLastCommented() { return lastCommented; }
    public void setLastCommented(String lastCommented) { this.lastCommented = lastCommented; }

    public String getLastLiked() { return lastLiked; }
    public void setLastLiked(String lastLiked) { this.lastLiked = lastLiked; }

    public String getLastPosted() { return lastPosted; }
    public void setLastPosted(String lastPosted) { this.lastPosted = lastPosted; }

    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }


}