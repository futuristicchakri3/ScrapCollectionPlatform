package com.scrapconnection.dto;

import java.math.BigDecimal;

public class SettlementItemDto {

    private Long materialId; // BookingMaterial id
    private Long categoryId;
    private String categoryName;
    private BigDecimal actualWeightKg = BigDecimal.ZERO;
    private BigDecimal pricePerKg = BigDecimal.ZERO;

    public SettlementItemDto() {
    }

    public SettlementItemDto(Long materialId, Long categoryId, String categoryName, BigDecimal actualWeightKg, BigDecimal pricePerKg) {
        this.materialId = materialId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.actualWeightKg = actualWeightKg;
        this.pricePerKg = pricePerKg;
    }

    // Getters and Setters
    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getActualWeightKg() {
        return actualWeightKg;
    }

    public void setActualWeightKg(BigDecimal actualWeightKg) {
        this.actualWeightKg = actualWeightKg;
    }

    public BigDecimal getPricePerKg() {
        return pricePerKg;
    }

    public void setPricePerKg(BigDecimal pricePerKg) {
        this.pricePerKg = pricePerKg;
    }
}
