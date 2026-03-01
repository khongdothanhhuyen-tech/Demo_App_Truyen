package com.example.app_truyen.Models;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Comment {
    private String id, uid, tenHienThi, avatarUrl, noiDung;
    private List<String> danhSachLikes;
    private Timestamp ngayDang;
    private String commentImage; // TRƯỜNG MỚI ĐỂ LƯU ẢNH TRONG CMT

    // Giữ nguyên các trường đính kèm truyện cũ của đạo hữu
    private String storyIdDinhKem, tenTruyenDinhKem, anhTruyenDinhKem;

    public Comment() { this.danhSachLikes = new ArrayList<>(); }

    // Cập nhật Constructor đầy đủ
    public Comment(String id, String uid, String tenHienThi, String avatarUrl, String noiDung, List<String> danhSachLikes, Timestamp ngayDang) {
        this.id = id;
        this.uid = uid;
        this.tenHienThi = tenHienThi;
        this.avatarUrl = avatarUrl;
        this.noiDung = noiDung;
        this.danhSachLikes = (danhSachLikes != null) ? danhSachLikes : new ArrayList<>();
        this.ngayDang = ngayDang;
    }

    // Thêm Getter/Setter cho commentImage
    public String getCommentImage() { return commentImage; }
    public void setCommentImage(String commentImage) { this.commentImage = commentImage; }

    public void setTenHienThi(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }

    public void setDanhSachLikes(List<String> danhSachLikes) {
        this.danhSachLikes = danhSachLikes;
    }

    public void setNgayDang(Timestamp ngayDang) {
        this.ngayDang = ngayDang;
    }

    public String getStoryIdDinhKem() {
        return storyIdDinhKem;
    }

    public void setStoryIdDinhKem(String storyIdDinhKem) {
        this.storyIdDinhKem = storyIdDinhKem;
    }

    public String getTenTruyenDinhKem() {
        return tenTruyenDinhKem;
    }

    public void setTenTruyenDinhKem(String tenTruyenDinhKem) {
        this.tenTruyenDinhKem = tenTruyenDinhKem;
    }

    public String getAnhTruyenDinhKem() {
        return anhTruyenDinhKem;
    }

    public void setAnhTruyenDinhKem(String anhTruyenDinhKem) {
        this.anhTruyenDinhKem = anhTruyenDinhKem;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getTenHienThi() { return tenHienThi; }
    public String getNoiDung() { return noiDung; }
    public Timestamp getNgayDang() { return ngayDang; }
    public List<String> getDanhSachLikes() { return danhSachLikes != null ? danhSachLikes : new ArrayList<>(); }
}