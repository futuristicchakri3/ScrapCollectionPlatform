package com.scrapconnection.entity;

import com.scrapconnection.enums.PaymentStatus;
import com.scrapconnection.enums.SettlementMethod;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_payments")
public class OrderPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private CustomerOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SettlementMethod paymentMethod;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal couponAmountUsed = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus paymentStatus;

    @Column(length = 100)
    private String paymentReference;

    @Column(nullable = false, updatable = false)
    private LocalDateTime paidAt;

    public OrderPayment() {
    }

    public OrderPayment(CustomerOrder order, SettlementMethod paymentMethod, BigDecimal amountPaid,
                        BigDecimal couponAmountUsed, PaymentStatus paymentStatus, String paymentReference) {
        this.order = order;
        this.paymentMethod = paymentMethod;
        this.amountPaid = amountPaid;
        this.couponAmountUsed = couponAmountUsed != null ? couponAmountUsed : BigDecimal.ZERO;
        this.paymentStatus = paymentStatus;
        this.paymentReference = paymentReference;
        this.paidAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.paidAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public void setOrder(CustomerOrder order) {
        this.order = order;
    }

    public SettlementMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(SettlementMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public BigDecimal getCouponAmountUsed() {
        return couponAmountUsed;
    }

    public void setCouponAmountUsed(BigDecimal couponAmountUsed) {
        this.couponAmountUsed = couponAmountUsed;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
