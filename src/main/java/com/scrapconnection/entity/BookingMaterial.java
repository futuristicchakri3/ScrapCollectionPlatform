package com.scrapconnection.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "booking_materials")
public class BookingMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private ScrapBooking booking;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private MaterialCategory category;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedWeightKg = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal actualWeightKg = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerKg = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    public BookingMaterial() {
    }

    public BookingMaterial(ScrapBooking booking, MaterialCategory category, BigDecimal estimatedWeightKg) {
        this.booking = booking;
        this.category = category;
        this.estimatedWeightKg = estimatedWeightKg;
        this.actualWeightKg = BigDecimal.ZERO;
        this.pricePerKg = BigDecimal.ZERO;
        this.lineTotal = BigDecimal.ZERO;
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

    public MaterialCategory getCategory() {
        return category;
    }

    public void setCategory(MaterialCategory category) {
        this.category = category;
    }

    public BigDecimal getEstimatedWeightKg() {
        return estimatedWeightKg;
    }

    public void setEstimatedWeightKg(BigDecimal estimatedWeightKg) {
        this.estimatedWeightKg = estimatedWeightKg;
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

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
