package com.scrapconnection.entity;

import com.scrapconnection.enums.BookingStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scrap_bookings", indexes = {
    @Index(name = "idx_booking_ref", columnList = "bookingReference", unique = true),
    @Index(name = "idx_booking_status", columnList = "status"),
    @Index(name = "idx_booking_locality", columnList = "locality")
})
public class ScrapBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String bookingReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_profile_id", nullable = false)
    private CustomerProfile customerProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_shopkeeper_id")
    private ShopkeeperProfile assignedShopkeeper;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status = BookingStatus.REQUESTED;

    @Column(nullable = false, length = 255)
    private String pickupAddress;

    @Column(nullable = false, length = 100)
    private String locality;

    @Column(nullable = false, length = 10)
    private String postalCode;

    @Column(nullable = false)
    private LocalDate preferredDate;

    @Column(nullable = false, length = 50)
    private String timeSlot;

    @Column(nullable = false, length = 15)
    private String contactNumber;

    @Column(columnDefinition = "TEXT")
    private String additionalInstructions;

    @Column(length = 255)
    private String scrapImageUrl;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalEstimatedWeight = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal actualTotalWeight = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Version
    private Long version = 0L;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingMaterial> materials = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingStatusHistory> statusHistories = new ArrayList<>();

    public ScrapBooking() {
    }

    public ScrapBooking(String bookingReference, CustomerProfile customerProfile, String pickupAddress,
                        String locality, String postalCode, LocalDate preferredDate, String timeSlot,
                        String contactNumber, String additionalInstructions, String scrapImageUrl) {
        this.bookingReference = bookingReference;
        this.customerProfile = customerProfile;
        this.pickupAddress = pickupAddress;
        this.locality = locality;
        this.postalCode = postalCode;
        this.preferredDate = preferredDate;
        this.timeSlot = timeSlot;
        this.contactNumber = contactNumber;
        this.additionalInstructions = additionalInstructions;
        this.scrapImageUrl = scrapImageUrl;
        this.status = BookingStatus.REQUESTED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addMaterial(BookingMaterial material) {
        materials.add(material);
        material.setBooking(this);
    }

    public void addStatusHistory(BookingStatusHistory history) {
        statusHistories.add(history);
        history.setBooking(this);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public CustomerProfile getCustomerProfile() {
        return customerProfile;
    }

    public void setCustomerProfile(CustomerProfile customerProfile) {
        this.customerProfile = customerProfile;
    }

    public ShopkeeperProfile getAssignedShopkeeper() {
        return assignedShopkeeper;
    }

    public void setAssignedShopkeeper(ShopkeeperProfile assignedShopkeeper) {
        this.assignedShopkeeper = assignedShopkeeper;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public LocalDate getPreferredDate() {
        return preferredDate;
    }

    public void setPreferredDate(LocalDate preferredDate) {
        this.preferredDate = preferredDate;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getAdditionalInstructions() {
        return additionalInstructions;
    }

    public void setAdditionalInstructions(String additionalInstructions) {
        this.additionalInstructions = additionalInstructions;
    }

    public String getScrapImageUrl() {
        return scrapImageUrl;
    }

    public void setScrapImageUrl(String scrapImageUrl) {
        this.scrapImageUrl = scrapImageUrl;
    }

    public BigDecimal getTotalEstimatedWeight() {
        return totalEstimatedWeight;
    }

    public void setTotalEstimatedWeight(BigDecimal totalEstimatedWeight) {
        this.totalEstimatedWeight = totalEstimatedWeight;
    }

    public BigDecimal getActualTotalWeight() {
        return actualTotalWeight;
    }

    public void setActualTotalWeight(BigDecimal actualTotalWeight) {
        this.actualTotalWeight = actualTotalWeight;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<BookingMaterial> getMaterials() {
        return materials;
    }

    public void setMaterials(List<BookingMaterial> materials) {
        this.materials = materials;
    }

    public List<BookingStatusHistory> getStatusHistories() {
        return statusHistories;
    }

    public void setStatusHistories(List<BookingStatusHistory> statusHistories) {
        this.statusHistories = statusHistories;
    }
}
