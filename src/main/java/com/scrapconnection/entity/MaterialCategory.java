package com.scrapconnection.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "material_categories")
public class MaterialCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // e.g., PAPER, CARDBOARD, PLASTIC, IRON, ALUMINIUM, COPPER, GLASS, E_WASTE

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(length = 255)
    private String description;

    @Column(length = 100)
    private String iconName; // e.g. newspaper, box, bottle, hammer, etc.

    @Column(nullable = false)
    private boolean active = true;

    public MaterialCategory() {
    }

    public MaterialCategory(String code, String displayName, String description, String iconName, boolean active) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        this.iconName = iconName;
        this.active = active;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
