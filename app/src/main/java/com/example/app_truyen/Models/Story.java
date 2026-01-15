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

}
