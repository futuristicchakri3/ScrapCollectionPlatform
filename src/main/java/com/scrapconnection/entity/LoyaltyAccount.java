package com.scrapconnection.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "loyalty_accounts")
public class LoyaltyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_profile_id", nullable = false, unique = true)
    private CustomerProfile customerProfile;

    @Column(nullable = false)
    private int totalPoints = 0;

    @Version
    private Long version = 0L;

    public LoyaltyAccount() {
    }

    public LoyaltyAccount(CustomerProfile customerProfile, int totalPoints) {
        this.customerProfile = customerProfile;
        this.totalPoints = totalPoints;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerProfile getCustomerProfile() {
        return customerProfile;
    }

    public void setCustomerProfile(CustomerProfile customerProfile) {
        this.customerProfile = customerProfile;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
