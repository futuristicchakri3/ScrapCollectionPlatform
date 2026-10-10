package com.scrapconnection.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SettlementDto {

    private Long bookingId;
    private List<SettlementItemDto> items = new ArrayList<>();
    private BigDecimal deductions = BigDecimal.ZERO;
    private String deductionReason;
    private String settlementMethod; // CASH, UPI, COUPON

    public SettlementDto() {
    }

    // Getters and Setters
    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public List<SettlementItemDto> getItems() {
        return items;
    }

    public void setItems(List<SettlementItemDto> items) {
        this.items = items;
    }

    public BigDecimal getDeductions() {
        return deductions;
    }

    public void setDeductions(BigDecimal deductions) {
        this.deductions = deductions;
    }

    public String getDeductionReason() {
        return deductionReason;
    }

    public void setDeductionReason(String deductionReason) {
        this.deductionReason = deductionReason;
    }

    public String getSettlementMethod() {
        return settlementMethod;
    }

    public void setSettlementMethod(String settlementMethod) {
        this.settlementMethod = settlementMethod;
    }
}
