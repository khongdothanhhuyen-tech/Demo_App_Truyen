package com.example.app_truyen.Models;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Post implements Serializable {
    private String postId, userId, userName, userAvatar, content, postImage;
    private Timestamp timestamp;
    private List<String> likes;
    private int commentCount; // Thêm để thống kê số bình luận

    public Post() {
        this.likes = new ArrayList<>();
        this.commentCount = 0;
    }

    public Post(String postId, String userId, String userName, String userAvatar, String content, String postImage, Timestamp timestamp) {
        this.postId = postId;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.content = content;
        this.postImage = postImage;
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
    public String getPostImage() { return postImage; }
    public void setPostImage(String postImage) { this.postImage = postImage; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public List<String> getLikes() { return likes != null ? likes : new ArrayList<>(); }
    public void setLikes(List<String> likes) { this.likes = likes; }
    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
}