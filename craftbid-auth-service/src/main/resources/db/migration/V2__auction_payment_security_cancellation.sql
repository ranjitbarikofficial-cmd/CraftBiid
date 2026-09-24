-- ==============================================================================
-- CRAFTBID PRODUCTION DATABASE MIGRATION V2
-- Auction Security, Voluntary Cancellation, Differential Bids & Settlements
-- Standard DDL for MySQL 8.0+ / 8.4+ / 9.0+
-- ==============================================================================

-- 1. AUCTION PARTICIPANTS: Add voluntary cancellation tracking columns
ALTER TABLE auction_participants
    ADD COLUMN cancellation_fee DECIMAL(12, 2) DEFAULT 0.00,
    ADD COLUMN cancellation_refund_amount DECIMAL(12, 2) DEFAULT 0.00,
    ADD COLUMN cancellation_status VARCHAR(50) DEFAULT NULL,
    ADD COLUMN cancelled_at DATETIME(6) DEFAULT NULL,
    ADD COLUMN cancellation_requested_at DATETIME(6) DEFAULT NULL;

-- 2. AUCTIONS: Add auction lifecycle countdown deadlines and platform/artisan payout split columns
ALTER TABLE auctions
    ADD COLUMN first_deposit_paid_at DATETIME(6) DEFAULT NULL,
    ADD COLUMN participation_deadline DATETIME(6) DEFAULT NULL,
    ADD COLUMN prep_deadline DATETIME(6) DEFAULT NULL,
    ADD COLUMN initial_wait_deadline DATETIME(6) DEFAULT NULL,
    ADD COLUMN turn_deadline DATETIME(6) DEFAULT NULL,
    ADD COLUMN admin_fee_amount DECIMAL(12, 2) DEFAULT NULL,
    ADD COLUMN artisan_payout_amount DECIMAL(12, 2) DEFAULT NULL;

-- 3. SELLER SETTLEMENTS: Add manual payout tracking columns (bank transfer reference / UTR)
ALTER TABLE seller_settlements
    ADD COLUMN payout_reference VARCHAR(100) DEFAULT NULL,
    ADD COLUMN payout_method VARCHAR(50) DEFAULT 'BANK_TRANSFER',
    ADD COLUMN settled_at DATETIME(6) DEFAULT NULL,
    ADD COLUMN notes TEXT DEFAULT NULL;
