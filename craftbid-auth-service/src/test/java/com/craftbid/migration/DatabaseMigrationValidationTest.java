package com.craftbid.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseMigrationValidationTest {

    private static final String DB_URL_BASE = "jdbc:mysql://localhost:3306/";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "ranjitbarik";

    @Test
    @DisplayName("Scenario 1: Fresh Database -> Flyway V1 -> V2 -> Schema Verification")
    public void testScenario1_FreshDatabase_FlywayMigration() throws Exception {
        String dbName = "craftbid_test_flyway_s1";
        try (Connection conn = DriverManager.getConnection(DB_URL_BASE, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
            stmt.executeUpdate("CREATE DATABASE " + dbName);
        }

        String dbUrl = DB_URL_BASE + dbName + "?useSSL=false&allowPublicKeyRetrieval=true";

        Flyway flyway = Flyway.configure()
                .dataSource(dbUrl, USERNAME, PASSWORD)
                .locations("classpath:db/migration")
                .load();

        int appliedMigrations = flyway.migrate().migrationsExecuted;
        assertEquals(2, appliedMigrations, "Flyway should successfully execute V1 and V2 migrations");

        // Verify key columns exist in migrated schema
        try (Connection conn = DriverManager.getConnection(dbUrl, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {

            // Check auction_participants columns
            Set<String> participantCols = getColumns(stmt, "auction_participants");
            assertTrue(participantCols.contains("cancellation_fee"));
            assertTrue(participantCols.contains("cancellation_refund_amount"));
            assertTrue(participantCols.contains("cancellation_status"));
            assertTrue(participantCols.contains("cancelled_at"));
            assertTrue(participantCols.contains("cancellation_requested_at"));

            // Check auctions columns
            Set<String> auctionCols = getColumns(stmt, "auctions");
            assertTrue(auctionCols.contains("first_deposit_paid_at"));
            assertTrue(auctionCols.contains("participation_deadline"));
            assertTrue(auctionCols.contains("prep_deadline"));
            assertTrue(auctionCols.contains("turn_deadline"));
            assertTrue(auctionCols.contains("admin_fee_amount"));
            assertTrue(auctionCols.contains("artisan_payout_amount"));

            // Check seller_settlements columns
            Set<String> settlementCols = getColumns(stmt, "seller_settlements");
            assertTrue(settlementCols.contains("payout_reference"));
            assertTrue(settlementCols.contains("payout_method"));
            assertTrue(settlementCols.contains("settled_at"));
            assertTrue(settlementCols.contains("notes"));

            // Check payment_transactions columns & indexes
            Set<String> paymentCols = getColumns(stmt, "payment_transactions");
            assertTrue(paymentCols.contains("razorpay_order_id"));

            // Check unique constraint on auction_participants
            ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.table_constraints " +
                    "WHERE table_schema = '" + dbName + "' " +
                    "AND table_name = 'auction_participants' " +
                    "AND constraint_name = 'uk_auction_user_participant'");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }

        // Cleanup
        try (Connection conn = DriverManager.getConnection(DB_URL_BASE, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
        }
    }

    @Test
    @DisplayName("Scenario 2: Existing Database -> Baseline Version 1 -> Migrate V2")
    public void testScenario2_ExistingDatabase_BaselineAndMigrate() throws Exception {
        String dbName = "craftbid_test_flyway_s2";
        try (Connection conn = DriverManager.getConnection(DB_URL_BASE, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
            stmt.executeUpdate("CREATE DATABASE " + dbName);
        }

        String dbUrl = DB_URL_BASE + dbName + "?useSSL=false&allowPublicKeyRetrieval=true";

        // Step A: Simulate pre-existing database running V1 initial schema manually
        try (Connection conn = DriverManager.getConnection(dbUrl, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {
            // Run V1 manually to simulate existing production DB without Flyway
            stmt.executeUpdate("CREATE TABLE users (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) NOT NULL, email VARCHAR(255) UNIQUE, password VARCHAR(255) NOT NULL, seller_enabled BOOLEAN NOT NULL DEFAULT FALSE, phone VARCHAR(255) UNIQUE, role VARCHAR(50) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE, otp VARCHAR(255) DEFAULT NULL, otp_expiry DATETIME(6) DEFAULT NULL, profile_image_url VARCHAR(500) DEFAULT NULL, city VARCHAR(255) DEFAULT 'India')");
            stmt.executeUpdate("CREATE TABLE categories (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE, description VARCHAR(500) DEFAULT NULL, image_url VARCHAR(500) DEFAULT NULL)");
            stmt.executeUpdate("CREATE TABLE crafts (id BIGINT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(150) NOT NULL, description VARCHAR(2000) DEFAULT NULL, base_price DECIMAL(12, 2) NOT NULL, image_url VARCHAR(500) DEFAULT NULL, status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', seller_id BIGINT NOT NULL, category_id BIGINT NOT NULL, created_at DATETIME(6) NOT NULL, FOREIGN KEY (seller_id) REFERENCES users (id), FOREIGN KEY (category_id) REFERENCES categories (id))");
            stmt.executeUpdate("CREATE TABLE auctions (id BIGINT AUTO_INCREMENT PRIMARY KEY, craft_id BIGINT NOT NULL, seller_id BIGINT NOT NULL, starting_price DECIMAL(12, 2) NOT NULL, current_highest_bid DECIMAL(12, 2) DEFAULT NULL, reserve_price DECIMAL(12, 2) DEFAULT NULL, min_bid_increment DECIMAL(12, 2) NOT NULL DEFAULT 50.00, start_time DATETIME(6) NOT NULL, end_time DATETIME(6) NOT NULL, status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', winning_bidder_id BIGINT DEFAULT NULL, total_bids INT NOT NULL DEFAULT 0, max_participants INT NOT NULL DEFAULT 5, current_participants_count INT NOT NULL DEFAULT 0, interested_count INT NOT NULL DEFAULT 0, last_bid_time DATETIME(6) DEFAULT NULL, live_turn_active BOOLEAN NOT NULL DEFAULT FALSE, created_at DATETIME(6) NOT NULL, FOREIGN KEY (craft_id) REFERENCES crafts (id), FOREIGN KEY (seller_id) REFERENCES users (id))");
            stmt.executeUpdate("CREATE TABLE auction_participants (id BIGINT AUTO_INCREMENT PRIMARY KEY, auction_id BIGINT NOT NULL, user_id BIGINT NOT NULL, base_price_paid DECIMAL(12, 2) NOT NULL, total_amount_paid DECIMAL(12, 2) NOT NULL, status VARCHAR(50) NOT NULL DEFAULT 'JOINED', refund_amount DECIMAL(12, 2) DEFAULT 0.00, joined_at DATETIME(6) NOT NULL, CONSTRAINT uk_auction_user_participant UNIQUE (auction_id, user_id), FOREIGN KEY (auction_id) REFERENCES auctions (id), FOREIGN KEY (user_id) REFERENCES users (id))");
            stmt.executeUpdate("CREATE TABLE addresses (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, full_name VARCHAR(100) NOT NULL, phone VARCHAR(15) NOT NULL, address_line1 VARCHAR(255) NOT NULL, address_line2 VARCHAR(255) DEFAULT NULL, city VARCHAR(100) NOT NULL, state VARCHAR(100) NOT NULL, pincode VARCHAR(10) NOT NULL, landmark VARCHAR(150) DEFAULT NULL, is_default BOOLEAN NOT NULL DEFAULT FALSE, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) DEFAULT NULL, FOREIGN KEY (user_id) REFERENCES users (id))");
            stmt.executeUpdate("CREATE TABLE auction_orders (id BIGINT AUTO_INCREMENT PRIMARY KEY, order_number VARCHAR(64) UNIQUE, auction_id BIGINT NOT NULL UNIQUE, buyer_id BIGINT NOT NULL, artisan_id BIGINT NOT NULL, winning_amount DECIMAL(12, 2) NOT NULL, shipping_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00, total_amount DECIMAL(12, 2) NOT NULL, platform_fee DECIMAL(12, 2) NOT NULL, artisan_payout DECIMAL(12, 2) NOT NULL, shipping_address_id BIGINT DEFAULT NULL, full_name VARCHAR(255) DEFAULT NULL, street_address VARCHAR(255) DEFAULT NULL, city VARCHAR(255) DEFAULT NULL, state VARCHAR(255) DEFAULT NULL, pincode VARCHAR(255) DEFAULT NULL, phone VARCHAR(255) DEFAULT NULL, landmark VARCHAR(255) DEFAULT NULL, status VARCHAR(50) NOT NULL DEFAULT 'ADDRESS_REQUIRED', notes VARCHAR(500) DEFAULT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) DEFAULT NULL, FOREIGN KEY (auction_id) REFERENCES auctions (id), FOREIGN KEY (buyer_id) REFERENCES users (id), FOREIGN KEY (artisan_id) REFERENCES users (id))");
            stmt.executeUpdate("CREATE TABLE seller_settlements (id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL UNIQUE, artisan_id BIGINT NOT NULL, winning_amount DECIMAL(12, 2) NOT NULL, platform_commission_rate DECIMAL(5, 2) NOT NULL DEFAULT 10.00, platform_fee DECIMAL(12, 2) NOT NULL, artisan_payout DECIMAL(12, 2) NOT NULL, status VARCHAR(50) NOT NULL DEFAULT 'PENDING', artisan_bank_details VARCHAR(255) DEFAULT NULL, artisan_upi_id VARCHAR(100) DEFAULT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) DEFAULT NULL, FOREIGN KEY (order_id) REFERENCES auction_orders (id), FOREIGN KEY (artisan_id) REFERENCES users (id))");
        }

        // Step B: Now initialize Flyway with baseline-on-migrate=true, baseline-version=1
        Flyway flyway = Flyway.configure()
                .dataSource(dbUrl, USERNAME, PASSWORD)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();

        int applied = flyway.migrate().migrationsExecuted;
        assertEquals(1, applied, "Flyway should baseline at V1 and execute exactly 1 incremental migration (V2)");

        // Step C: Verify V2 columns are present
        try (Connection conn = DriverManager.getConnection(dbUrl, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {
            Set<String> participantCols = getColumns(stmt, "auction_participants");
            assertTrue(participantCols.contains("cancellation_fee"));
            assertTrue(participantCols.contains("cancellation_refund_amount"));

            Set<String> auctionCols = getColumns(stmt, "auctions");
            assertTrue(auctionCols.contains("first_deposit_paid_at"));
            assertTrue(auctionCols.contains("participation_deadline"));

            Set<String> settlementCols = getColumns(stmt, "seller_settlements");
            assertTrue(settlementCols.contains("payout_reference"));
        }

        // Cleanup
        try (Connection conn = DriverManager.getConnection(DB_URL_BASE, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
        }
    }

    private Set<String> getColumns(Statement stmt, String tableName) throws Exception {
        Set<String> cols = new HashSet<>();
        try (ResultSet rs = stmt.executeQuery("DESCRIBE " + tableName)) {
            while (rs.next()) {
                cols.add(rs.getString("Field"));
            }
        }
        return cols;
    }
}
