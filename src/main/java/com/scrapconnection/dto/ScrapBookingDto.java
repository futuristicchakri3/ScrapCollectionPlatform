package com.scrapconnection.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ScrapBookingDto {

    private List<BookingMaterialItemDto> materials = new ArrayList<>();

    // Quick single-category selection fallback for simple form submissions
    private String selectedCategory;
    private java.math.BigDecimal estimatedWeight;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotBlank(message = "Locality is required")
    private String locality;

    @NotBlank(message = "Postal code is required")
    @Pattern(regexp = "^\\d{6}$", message = "Postal code must be 6 digits")
    private String postalCode;

    @NotNull(message = "Preferred pickup date is required")
    @FutureOrPresent(message = "Pickup date cannot be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate preferredDate;

    @NotBlank(message = "Time slot is required")
    private String timeSlot;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Please enter a valid 10-digit mobile number")
    private String contactNumber;

    private String additionalInstructions;

    private String scrapImageUrl;

    public ScrapBookingDto() {
    }

    // Getters and Setters
    public List<BookingMaterialItemDto> getMaterials() {
        return materials;
    }

    public void setMaterials(List<BookingMaterialItemDto> materials) {
        this.materials = materials;
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(String selectedCategory) {
        this.selectedCategory = selectedCategory;
    }

    public java.math.BigDecimal getEstimatedWeight() {
        return estimatedWeight;
    }

    public void setEstimatedWeight(java.math.BigDecimal estimatedWeight) {
        this.estimatedWeight = estimatedWeight;
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
}
