package com.example.app_truyen.Models;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Post implements Serializable {
    private String postId, userId, userName, userAvatar, content;
    private Timestamp timestamp;
    private List<String> likes;
    private int commentCount;
    private List<String> postImages;

    public Post() {
        this.likes = new ArrayList<>();
        this.postImages = new ArrayList<>();
        this.commentCount = 0;
    }

    public Post(String postId, String userId, String userName, String userAvatar, String content, List<String> postImages, Timestamp timestamp) {
        this.postId = postId;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.content = content;
        this.postImages = postImages != null ? postImages : new ArrayList<>();
        this.timestamp = timestamp;
        this.likes = new ArrayList<>();
        this.commentCount = 0;
    }

    // Getters and Setters
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getPostImages() { return postImages != null ? postImages : new ArrayList<>(); }
    public void setPostImages(List<String> postImages) { this.postImages = postImages; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public List<String> getLikes() { return likes != null ? likes : new ArrayList<>(); }
    public void setLikes(List<String> likes) { this.likes = likes; }
    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
}