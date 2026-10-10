package com.scrapconnection.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class BookingMaterialItemDto {

    @NotBlank(message = "Material category is required")
    private String categoryCode;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.1", message = "Weight must be at least 0.1 kg")
    private BigDecimal estimatedWeightKg;

    public BookingMaterialItemDto() {
    }

    public BookingMaterialItemDto(String categoryCode, BigDecimal estimatedWeightKg) {
        this.categoryCode = categoryCode;
        this.estimatedWeightKg = estimatedWeightKg;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public BigDecimal getEstimatedWeightKg() {
        return estimatedWeightKg;
    }

    public void setEstimatedWeightKg(BigDecimal estimatedWeightKg) {
        this.estimatedWeightKg = estimatedWeightKg;
    }
}
