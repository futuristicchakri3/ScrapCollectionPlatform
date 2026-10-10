-- =============================================================================
-- SCRAP CONNECTION — MySQL SCHEMA DDL STATEMENTS
-- =============================================================================

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mobile_number VARCHAR(15) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_user_mobile (mobile_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Customer Profiles
CREATE TABLE IF NOT EXISTS customer_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    default_locality VARCHAR(100),
    default_address VARCHAR(255),
    created_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Shopkeeper Profiles
CREATE TABLE IF NOT EXISTS shopkeeper_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    owner_name VARCHAR(100) NOT NULL,
    shop_name VARCHAR(150) NOT NULL,
    shop_address VARCHAR(255) NOT NULL,
    locality VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(10) NOT NULL,
    email VARCHAR(100),
    shop_description TEXT,
    supported_categories VARCHAR(255),
    approval_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    approved_at DATETIME,
    rejection_reason VARCHAR(255),
    created_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Customer Addresses
CREATE TABLE IF NOT EXISTS addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_profile_id BIGINT NOT NULL,
    title VARCHAR(50) NOT NULL DEFAULT 'Home',
    street_address VARCHAR(255) NOT NULL,
    locality VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(10) NOT NULL,
    contact_number VARCHAR(15),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (customer_profile_id) REFERENCES customer_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. OTP Verifications
CREATE TABLE IF NOT EXISTS otp_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mobile_number VARCHAR(15) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    dev_otp VARCHAR(10),
    purpose VARCHAR(50) NOT NULL,
    expiry_time DATETIME NOT NULL,
    attempts_count INT NOT NULL DEFAULT 0,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    INDEX idx_otp_mobile (mobile_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Material Categories
CREATE TABLE IF NOT EXISTS material_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    icon_name VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Material Prices
CREATE TABLE IF NOT EXISTS material_prices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    price_per_kg DECIMAL(10,2) NOT NULL,
    effective_from DATETIME NOT NULL,
    effective_to DATETIME,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (category_id) REFERENCES material_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. Scrap Bookings
CREATE TABLE IF NOT EXISTS scrap_bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_reference VARCHAR(32) NOT NULL UNIQUE,
    customer_profile_id BIGINT NOT NULL,
    assigned_shopkeeper_id BIGINT,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    pickup_address VARCHAR(255) NOT NULL,
    locality VARCHAR(100) NOT NULL,
    postal_code VARCHAR(10) NOT NULL,
    preferred_date DATE NOT NULL,
    time_slot VARCHAR(50) NOT NULL,
    contact_number VARCHAR(15) NOT NULL,
    additional_instructions TEXT,
    scrap_image_url VARCHAR(255),
    total_estimated_weight DECIMAL(10,2) DEFAULT 0.00,
    actual_total_weight DECIMAL(10,2) DEFAULT 0.00,
    total_amount DECIMAL(10,2) DEFAULT 0.00,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_booking_ref (booking_reference),
    INDEX idx_booking_status (status),
    INDEX idx_booking_locality (locality),
    FOREIGN KEY (customer_profile_id) REFERENCES customer_profiles(id),
    FOREIGN KEY (assigned_shopkeeper_id) REFERENCES shopkeeper_profiles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. Booking Materials
CREATE TABLE IF NOT EXISTS booking_materials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    estimated_weight_kg DECIMAL(10,2) DEFAULT 0.00,
    actual_weight_kg DECIMAL(10,2) DEFAULT 0.00,
    price_per_kg DECIMAL(10,2) DEFAULT 0.00,
    line_total DECIMAL(10,2) DEFAULT 0.00,
    FOREIGN KEY (booking_id) REFERENCES scrap_bookings(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES material_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. Booking Status Histories
CREATE TABLE IF NOT EXISTS booking_status_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by_user_id BIGINT,
    remarks VARCHAR(255),
    timestamp DATETIME NOT NULL,
    FOREIGN KEY (booking_id) REFERENCES scrap_bookings(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. Settlements
CREATE TABLE IF NOT EXISTS settlements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    shopkeeper_id BIGINT NOT NULL,
    settlement_method VARCHAR(30) NOT NULL,
    gross_amount DECIMAL(10,2) NOT NULL,
    deductions DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    deduction_reason VARCHAR(255),
    net_amount DECIMAL(10,2) NOT NULL,
    settlement_status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    transaction_reference VARCHAR(100),
    settled_at DATETIME NOT NULL,
    FOREIGN KEY (booking_id) REFERENCES scrap_bookings(id),
    FOREIGN KEY (shopkeeper_id) REFERENCES shopkeeper_profiles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. Coupons
CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_code VARCHAR(50) NOT NULL UNIQUE,
    customer_profile_id BIGINT NOT NULL,
    issuing_shopkeeper_id BIGINT NOT NULL,
    settlement_id BIGINT,
    original_amount DECIMAL(10,2) NOT NULL,
    remaining_balance DECIMAL(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    issued_at DATETIME NOT NULL,
    expiry_date DATETIME,
    version BIGINT NOT NULL DEFAULT 0,
    INDEX idx_coupon_code (coupon_code),
    INDEX idx_coupon_customer (customer_profile_id),
    FOREIGN KEY (customer_profile_id) REFERENCES customer_profiles(id),
    FOREIGN KEY (issuing_shopkeeper_id) REFERENCES shopkeeper_profiles(id),
    FOREIGN KEY (settlement_id) REFERENCES settlements(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13. Coupon Ledgers
CREATE TABLE IF NOT EXISTS coupon_ledgers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    customer_profile_id BIGINT NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    balance_before DECIMAL(10,2) NOT NULL,
    balance_after DECIMAL(10,2) NOT NULL,
    reference_order_id BIGINT,
    description VARCHAR(255),
    timestamp DATETIME NOT NULL,
    FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    FOREIGN KEY (customer_profile_id) REFERENCES customer_profiles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14. Loyalty Accounts
CREATE TABLE IF NOT EXISTS loyalty_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_profile_id BIGINT NOT NULL UNIQUE,
    total_points INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (customer_profile_id) REFERENCES customer_profiles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 15. Loyalty Transactions
CREATE TABLE IF NOT EXISTS loyalty_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loyalty_account_id BIGINT NOT NULL,
    booking_id BIGINT,
    points INT NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    remarks VARCHAR(255),
    timestamp DATETIME NOT NULL,
    FOREIGN KEY (loyalty_account_id) REFERENCES loyalty_accounts(id),
    FOREIGN KEY (booking_id) REFERENCES scrap_bookings(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 16. Recycled Products
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    available_stock INT NOT NULL DEFAULT 0,
    image_url VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 17. Cart Items
CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_profile_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    added_at DATETIME NOT NULL,
    FOREIGN KEY (customer_profile_id) REFERENCES customer_profiles(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 18. Customer Orders
CREATE TABLE IF NOT EXISTS customer_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(32) NOT NULL UNIQUE,
    customer_profile_id BIGINT NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    coupon_discount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    payable_amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    order_status VARCHAR(30) NOT NULL DEFAULT 'PLACED',
    delivery_address VARCHAR(255) NOT NULL,
    contact_number VARCHAR(15) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_order_number (order_number),
    FOREIGN KEY (customer_profile_id) REFERENCES customer_profiles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 19. Order Items
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    line_total DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES customer_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 20. Order Payments
CREATE TABLE IF NOT EXISTS order_payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    payment_method VARCHAR(30) NOT NULL,
    amount_paid DECIMAL(10,2) NOT NULL,
    coupon_amount_used DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    payment_status VARCHAR(30) NOT NULL,
    payment_reference VARCHAR(100),
    paid_at DATETIME NOT NULL,
    FOREIGN KEY (order_id) REFERENCES customer_orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 21. Admin Audit Logs
CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    target_entity VARCHAR(100),
    target_entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(50),
    timestamp DATETIME NOT NULL,
    FOREIGN KEY (admin_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
