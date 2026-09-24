-- ==============================================================================
-- CRAFTBID PRODUCTION DATABASE MIGRATION V1: INITIAL BASELINE SCHEMA
-- Idempotent Base Table Creation for MySQL 8.0+
-- ==============================================================================

-- 1. USERS
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255) NOT NULL,
    seller_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    phone VARCHAR(255) UNIQUE,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    otp VARCHAR(255) DEFAULT NULL,
    otp_expiry DATETIME(6) DEFAULT NULL,
    profile_image_url VARCHAR(500) DEFAULT NULL,
    city VARCHAR(255) DEFAULT 'India'
);

-- 2. CATEGORIES
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500) DEFAULT NULL,
    image_url VARCHAR(500) DEFAULT NULL
);

-- 3. CRAFTS
CREATE TABLE IF NOT EXISTS crafts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(2000) DEFAULT NULL,
    base_price DECIMAL(12, 2) NOT NULL,
    image_url VARCHAR(500) DEFAULT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    seller_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_crafts_seller FOREIGN KEY (seller_id) REFERENCES users (id),
    CONSTRAINT fk_crafts_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

-- 4. ARTISAN PROFILES
CREATE TABLE IF NOT EXISTS artisan_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    shop_name VARCHAR(255) NOT NULL,
    craft_type VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    profile_image_url VARCHAR(500) DEFAULT NULL,
    bank_account_number VARCHAR(50) DEFAULT NULL,
    bank_ifsc_code VARCHAR(20) DEFAULT NULL,
    bank_account_name VARCHAR(100) DEFAULT NULL,
    upi_id VARCHAR(100) DEFAULT NULL,
    payout_preference VARCHAR(50) DEFAULT 'BANK_TRANSFER',
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_artisan_profile_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 5. CRAFT REELS
CREATE TABLE IF NOT EXISTS craft_reels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    artisan_id BIGINT NOT NULL,
    craft_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000) DEFAULT NULL,
    video_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500) DEFAULT NULL,
    views BIGINT NOT NULL DEFAULT 0,
    likes BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_craft_reels_artisan FOREIGN KEY (artisan_id) REFERENCES artisan_profiles (id),
    CONSTRAINT fk_craft_reels_craft FOREIGN KEY (craft_id) REFERENCES crafts (id)
);

-- 6. ADDRESSES
CREATE TABLE IF NOT EXISTS addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255) DEFAULT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    landmark VARCHAR(150) DEFAULT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 7. ARTISAN FOLLOWS
CREATE TABLE IF NOT EXISTS artisan_follows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    artisan_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_artisan_follow UNIQUE (follower_id, artisan_id),
    CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id),
    CONSTRAINT fk_follows_artisan FOREIGN KEY (artisan_id) REFERENCES users (id)
);

-- 8. AUCTIONS
CREATE TABLE IF NOT EXISTS auctions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    craft_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    starting_price DECIMAL(12, 2) NOT NULL,
    current_highest_bid DECIMAL(12, 2) DEFAULT NULL,
    reserve_price DECIMAL(12, 2) DEFAULT NULL,
    min_bid_increment DECIMAL(12, 2) NOT NULL DEFAULT 50.00,
    start_time DATETIME(6) NOT NULL,
    end_time DATETIME(6) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    winning_bidder_id BIGINT DEFAULT NULL,
    total_bids INT NOT NULL DEFAULT 0,
    max_participants INT NOT NULL DEFAULT 5,
    current_participants_count INT NOT NULL DEFAULT 0,
    interested_count INT NOT NULL DEFAULT 0,
    last_bid_time DATETIME(6) DEFAULT NULL,
    live_turn_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_auctions_craft FOREIGN KEY (craft_id) REFERENCES crafts (id),
    CONSTRAINT fk_auctions_seller FOREIGN KEY (seller_id) REFERENCES users (id),
    CONSTRAINT fk_auctions_winner FOREIGN KEY (winning_bidder_id) REFERENCES users (id)
);

-- 9. AUCTION PARTICIPANTS
CREATE TABLE IF NOT EXISTS auction_participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    base_price_paid DECIMAL(12, 2) NOT NULL,
    total_amount_paid DECIMAL(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'JOINED',
    refund_amount DECIMAL(12, 2) DEFAULT 0.00,
    joined_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_auction_user_participant UNIQUE (auction_id, user_id),
    CONSTRAINT fk_participants_auction FOREIGN KEY (auction_id) REFERENCES auctions (id),
    CONSTRAINT fk_participants_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 10. AUCTION INTERESTS
CREATE TABLE IF NOT EXISTS auction_interests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_auction_interest_user UNIQUE (auction_id, user_id),
    CONSTRAINT fk_interests_auction FOREIGN KEY (auction_id) REFERENCES auctions (id),
    CONSTRAINT fk_interests_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 11. BIDS
CREATE TABLE IF NOT EXISTS bids (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id BIGINT NOT NULL,
    bidder_id BIGINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    bid_time DATETIME(6) NOT NULL,
    status VARCHAR(100) DEFAULT NULL,
    CONSTRAINT fk_bids_auction FOREIGN KEY (auction_id) REFERENCES auctions (id),
    CONSTRAINT fk_bids_bidder FOREIGN KEY (bidder_id) REFERENCES users (id)
);

-- 12. AUCTION ORDERS
CREATE TABLE IF NOT EXISTS auction_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(64) UNIQUE,
    auction_id BIGINT NOT NULL UNIQUE,
    buyer_id BIGINT NOT NULL,
    artisan_id BIGINT NOT NULL,
    winning_amount DECIMAL(12, 2) NOT NULL,
    shipping_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12, 2) NOT NULL,
    platform_fee DECIMAL(12, 2) NOT NULL,
    artisan_payout DECIMAL(12, 2) NOT NULL,
    shipping_address_id BIGINT DEFAULT NULL,
    full_name VARCHAR(255) DEFAULT NULL,
    street_address VARCHAR(255) DEFAULT NULL,
    city VARCHAR(255) DEFAULT NULL,
    state VARCHAR(255) DEFAULT NULL,
    pincode VARCHAR(255) DEFAULT NULL,
    phone VARCHAR(255) DEFAULT NULL,
    landmark VARCHAR(255) DEFAULT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ADDRESS_REQUIRED',
    notes VARCHAR(500) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    CONSTRAINT fk_orders_auction FOREIGN KEY (auction_id) REFERENCES auctions (id),
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES users (id),
    CONSTRAINT fk_orders_artisan FOREIGN KEY (artisan_id) REFERENCES users (id),
    CONSTRAINT fk_orders_address FOREIGN KEY (shipping_address_id) REFERENCES addresses (id)
);

-- 13. SHIPMENTS
CREATE TABLE IF NOT EXISTS shipments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    provider VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    courier_name VARCHAR(100) DEFAULT NULL,
    tracking_number VARCHAR(100) DEFAULT NULL,
    package_weight DOUBLE DEFAULT NULL,
    package_length DOUBLE DEFAULT NULL,
    package_width DOUBLE DEFAULT NULL,
    package_height DOUBLE DEFAULT NULL,
    shipping_cost DECIMAL(10, 2) DEFAULT 0.00,
    pickup_date DATETIME(6) DEFAULT NULL,
    estimated_delivery_date DATETIME(6) DEFAULT NULL,
    picked_up_at DATETIME(6) DEFAULT NULL,
    delivered_at DATETIME(6) DEFAULT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    tracking_notes VARCHAR(500) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES auction_orders (id)
);

-- 14. SELLER SETTLEMENTS
CREATE TABLE IF NOT EXISTS seller_settlements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    artisan_id BIGINT NOT NULL,
    winning_amount DECIMAL(12, 2) NOT NULL,
    platform_commission_rate DECIMAL(5, 2) NOT NULL DEFAULT 10.00,
    platform_fee DECIMAL(12, 2) NOT NULL,
    artisan_payout DECIMAL(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    artisan_bank_details VARCHAR(255) DEFAULT NULL,
    artisan_upi_id VARCHAR(100) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    CONSTRAINT fk_settlements_order FOREIGN KEY (order_id) REFERENCES auction_orders (id),
    CONSTRAINT fk_settlements_artisan FOREIGN KEY (artisan_id) REFERENCES users (id)
);

-- 15. PAYMENT TRANSACTIONS
CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    auction_id BIGINT DEFAULT NULL,
    craft_id BIGINT DEFAULT NULL,
    razorpay_order_id VARCHAR(100) DEFAULT NULL,
    razorpay_payment_id VARCHAR(100) DEFAULT NULL,
    razorpay_signature VARCHAR(255) DEFAULT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    payment_type VARCHAR(255) NOT NULL,
    payment_method VARCHAR(255) NOT NULL DEFAULT 'RAZORPAY',
    transaction_ref VARCHAR(120) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    receipt VARCHAR(255) DEFAULT NULL,
    notes VARCHAR(1000) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    CONSTRAINT fk_payment_tx_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_payment_user (user_id),
    INDEX idx_payment_auction (auction_id),
    INDEX idx_payment_order_id (razorpay_order_id),
    INDEX idx_payment_payment_id (razorpay_payment_id),
    INDEX idx_payment_status (status)
);

-- 16. REFUNDS
CREATE TABLE IF NOT EXISTS refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT DEFAULT NULL,
    user_id BIGINT NOT NULL,
    auction_id BIGINT DEFAULT NULL,
    razorpay_payment_id VARCHAR(100) DEFAULT NULL,
    razorpay_refund_id VARCHAR(100) UNIQUE,
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    reason VARCHAR(500) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payment_transactions (id),
    CONSTRAINT fk_refunds_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_refund_payment (payment_id),
    INDEX idx_refund_user (user_id),
    INDEX idx_refund_auction (auction_id),
    INDEX idx_refund_rzp_refund_id (razorpay_refund_id),
    INDEX idx_refund_status (status)
);

-- 17. NOTIFICATIONS
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(255) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    link VARCHAR(255) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 18. SUPPORT TICKETS
CREATE TABLE IF NOT EXISTS support_tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT DEFAULT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(255) DEFAULT NULL,
    category VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    ticket_ref VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(255) NOT NULL DEFAULT 'OPEN',
    resolution_notes VARCHAR(255) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_support_tickets_user FOREIGN KEY (user_id) REFERENCES users (id)
);
