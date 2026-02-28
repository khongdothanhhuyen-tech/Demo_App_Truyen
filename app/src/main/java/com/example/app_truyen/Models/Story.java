package com.example.app_truyen.Models;

import java.io.Serializable;
import java.util.List;

public class Story implements Serializable {
    private String maTruyen;
    private String tenTruyen;
    private List<String> theLoai;
    private String tacGia;
    private String moTa ;
    private String anhBiaUrl ;

    private Boolean allowComment;
    private int viewCount;
    private int viewCountAll = 0;
    private int viewCountMonth = 0;
    private int viewCountWeek = 0;
    private String monthKey = "";
    private String weekKey = "";

    public Story() {}
    public Story(String maTruyen , String tenTruyen , List<String> theLoai , String tacGia , String moTa , String anhBiaUrl) {
        this.maTruyen = maTruyen;
        this.tenTruyen = tenTruyen;
        this.theLoai = theLoai;
        this.tacGia = tacGia;
        this.moTa = moTa;
        this.anhBiaUrl = anhBiaUrl;
    }
    public String getMaTruyen() {
        return maTruyen;
    }
    public void setMaTruyen(String maTruyen) {
        this.maTruyen = maTruyen;
    }
    public String getTenTruyen() {
        return tenTruyen;
    }
    public void setTenTruyen(String tenTruyen) {
        this.tenTruyen = tenTruyen;
    }
    public List<String> getTheLoai() {
        return theLoai;
    }
    public void setTheLoai(List<String> theLoai) {
        this.theLoai = theLoai;
    }
    public String getTacGia() {
        return tacGia;
    }
    public void setTacGia(String tacGia) {
        this.tacGia = tacGia;
    }
    public String getMoTa() {
        return moTa;
    }
    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }
    public String getAnhBiaUrl() {
        return anhBiaUrl;
    }
    public void setAnhBiaUrl(String anhBiaUrl) {
        this.anhBiaUrl = anhBiaUrl;
    }
    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public int getViewCountAll() {
        return viewCountAll;
    }

    public void setViewCountAll(int viewCountAll) {
        this.viewCountAll = viewCountAll;
    }

    public int getViewCountMonth() {
        return viewCountMonth;
    }

    public void setViewCountMonth(int viewCountMonth) {
        this.viewCountMonth = viewCountMonth;
    }

    public int getViewCountWeek() {
        return viewCountWeek;
    }

    public void setViewCountWeek(int viewCountWeek) {
        this.viewCountWeek = viewCountWeek;
    }

    public String getMonthKey() {
        return monthKey;
    }

    public void setMonthKey(String monthKey) {
        this.monthKey = monthKey;
    }

    public String getWeekKey() {
        return weekKey;
    }

    public void setWeekKey(String weekKey) {
        this.weekKey = weekKey;
    }
}
