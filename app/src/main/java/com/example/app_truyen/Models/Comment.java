package com.example.app_truyen.Models;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Comment {
    private String id;
    private String uid;
    private String tenHienThi;
    private String avatarUrl;
    private String noiDung;
    private List<String> danhSachLikes;
    private Timestamp ngayDang;

    public Comment() {
        this.danhSachLikes = new ArrayList<>();
    }
    public Comment(String id, String uid, String tenHienThi, String avatarUrl, String noiDung, List<String> danhSachLikes, Timestamp ngayDang) {
        this.id = id;
        this.uid = uid;
        this.tenHienThi = tenHienThi;
        this.avatarUrl = avatarUrl;
        this.noiDung = noiDung;
        this.danhSachLikes = (danhSachLikes != null) ? danhSachLikes : new ArrayList<>();
        this.ngayDang = ngayDang;
    }


    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getTenHienThi() { return tenHienThi; }
    public void setTenHienThi(String tenHienThi) { this.tenHienThi = tenHienThi; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getNoiDung() { return noiDung; }
    public void setNoiDung(String noiDung) { this.noiDung = noiDung; }

    public Timestamp getNgayDang() { return ngayDang; }
    public void setNgayDang(Timestamp ngayDang) { this.ngayDang = ngayDang; }


    public List<String> getDanhSachLikes() {
        if (danhSachLikes == null) {
            return new ArrayList<>();
        }
        return danhSachLikes;
    }
    public void setDanhSachLikes(List<String> danhSachLikes) {
        this.danhSachLikes = danhSachLikes;
    }
    public int getSoLuotThich() {
        if (danhSachLikes == null) return 0;
        return danhSachLikes.size();
    }
}