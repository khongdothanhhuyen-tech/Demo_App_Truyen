package com.example.app_truyen.Models;

import java.io.Serializable;
import java.util.List;

public class Chapter implements Serializable {
    private String id;
    private String tenChuong;
    private List<String> anhChuong;

    public Chapter() {}
    public Chapter(String id, String tenChuong, List<String> anhChuong) {
        this.id = id;
        this.tenChuong = tenChuong;
        this.anhChuong = anhChuong;
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
}
