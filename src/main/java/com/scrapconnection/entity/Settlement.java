package com.scrapconnection.entity;

import com.scrapconnection.enums.PaymentStatus;
import com.scrapconnection.enums.SettlementMethod;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlements", indexes = {
    @Index(name = "idx_settlement_booking", columnList = "booking_id", unique = true)
})
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private ScrapBooking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shopkeeper_id", nullable = false)
    private ShopkeeperProfile shopkeeper;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SettlementMethod settlementMethod;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deductions = BigDecimal.ZERO;

    @Column(length = 255)
    private String deductionReason;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus settlementStatus = PaymentStatus.CONFIRMED;

    @Column(length = 100)
    private String transactionReference;

    @Column(nullable = false, updatable = false)
    private LocalDateTime settledAt;

    public Settlement() {
    }

    public Settlement(ScrapBooking booking, ShopkeeperProfile shopkeeper, SettlementMethod settlementMethod,
                      BigDecimal grossAmount, BigDecimal deductions, String deductionReason,
                      BigDecimal netAmount, PaymentStatus settlementStatus, String transactionReference) {
        this.booking = booking;
        this.shopkeeper = shopkeeper;
        this.settlementMethod = settlementMethod;
        this.grossAmount = grossAmount;
        this.deductions = deductions != null ? deductions : BigDecimal.ZERO;
        this.deductionReason = deductionReason;
        this.netAmount = netAmount;
        this.settlementStatus = settlementStatus;
        this.transactionReference = transactionReference;
        this.settledAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.settledAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ScrapBooking getBooking() {
        return booking;
    }

    public void setBooking(ScrapBooking booking) {
        this.booking = booking;
    }

    public ShopkeeperProfile getShopkeeper() {
        return shopkeeper;
    }

    public void setShopkeeper(ShopkeeperProfile shopkeeper) {
        this.shopkeeper = shopkeeper;
    }

    public SettlementMethod getSettlementMethod() {
        return settlementMethod;
    }

    public void setSettlementMethod(SettlementMethod settlementMethod) {
        this.settlementMethod = settlementMethod;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
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

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public PaymentStatus getSettlementStatus() {
        return settlementStatus;
    }

    public void setSettlementStatus(PaymentStatus settlementStatus) {
        this.settlementStatus = settlementStatus;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(LocalDateTime settledAt) {
        this.settledAt = settledAt;
    }
}
