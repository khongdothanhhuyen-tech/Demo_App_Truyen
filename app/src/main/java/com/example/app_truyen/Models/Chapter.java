package com.example.app_truyen.Models;

import java.io.Serializable;
import java.util.List;


public class Chapter implements Serializable {
    private String id;
    private String tenChuong;
    private List<String> anhChuong;
    private long publishTime; // lưu dạng timestamp millis

    ///
    private String pdfUrl;

    public Chapter() {}
    // Constructor dùng cho chương ảnh
    public Chapter(String id, String tenChuong, List<String> anhChuong) {
        this.id = id;
        this.tenChuong = tenChuong;
        this.anhChuong = anhChuong;
        this.pdfUrl = null;
    }
    // Constructor dùng cho chương PDF
    public Chapter(String id, String tenChuong, String pdfUrl) {
        this.id = id;
        this.tenChuong = tenChuong;
        this.anhChuong = null;
        this.pdfUrl = pdfUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenChuong() {
        return tenChuong;
    }
    public void setTenChuong(String tenChuong) {
        this.tenChuong = tenChuong;
    }
    public List<String> getAnhChuong() {
        return anhChuong;
    }
    public void setAnhChuong(List<String> anhChuong) {
        this.anhChuong = anhChuong;
    }

    // GETTER + SETTER PDF
    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public long getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(long publishTime) {
        this.publishTime = publishTime;
    }

}
