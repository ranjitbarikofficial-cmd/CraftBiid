package com.craftbid.validation;

import com.craftbid.dto.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Input Schema Validation Tests")
public class InputValidationTest {

    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("RegisterRequest Validation")
    class RegisterRequestValidation {

        @Test
        @DisplayName("Should accept valid register request")
        void shouldAcceptValidRegisterRequest() {
            RegisterRequest request = new RegisterRequest();
            request.setName("John Doe");
            request.setEmail("john.doe@example.com");
            request.setPhone("9876543210");
            request.setPassword("StrongPass123");
            request.setRole("CUSTOMER");

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Expected no violations for valid RegisterRequest");
        }

        @Test
        @DisplayName("Should reject blank or invalid name")
        void shouldRejectInvalidName() {
            RegisterRequest request = new RegisterRequest();
            request.setName("A"); // Too short (< 2)
            request.setEmail("john@example.com");
            request.setPassword("Password123");

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
        }

        @Test
        @DisplayName("Should reject malformed email")
        void shouldRejectMalformedEmail() {
            RegisterRequest request = new RegisterRequest();
            request.setName("John Doe");
            request.setEmail("not-an-email");
            request.setPassword("Password123");

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        }

        @Test
        @DisplayName("Should reject invalid phone format")
        void shouldRejectInvalidPhone() {
            RegisterRequest request = new RegisterRequest();
            request.setName("John Doe");
            request.setEmail("john@example.com");
            request.setPhone("12345"); // too short
            request.setPassword("Password123");

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("phone")));
        }

        @Test
        @DisplayName("Should reject short password")
        void shouldRejectShortPassword() {
            RegisterRequest request = new RegisterRequest();
            request.setName("John Doe");
            request.setEmail("john@example.com");
            request.setPassword("123"); // < 6 chars

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
        }

        @Test
        @DisplayName("Should reject invalid role")
        void shouldRejectInvalidRole() {
            RegisterRequest request = new RegisterRequest();
            request.setName("John Doe");
            request.setEmail("john@example.com");
            request.setPassword("Password123");
            request.setRole("SUPERUSER"); // Invalid enum role

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("role")));
        }
    }

    @Nested
    @DisplayName("LoginRequest Validation")
    class LoginRequestValidation {

        @Test
        @DisplayName("Should accept valid login request")
        void shouldAcceptValidLogin() {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("john@example.com");
            request.setPassword("password123");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should reject blank identifier and password")
        void shouldRejectBlankIdentifier() {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("");
            request.setPassword("");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("identifier")));
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
        }
    }

    @Nested
    @DisplayName("EmailVerificationRequest Validation")
    class EmailVerificationRequestValidation {

        @Test
        @DisplayName("Should accept valid 6-digit OTP")
        void shouldAcceptValidOtp() {
            EmailVerificationRequest request = new EmailVerificationRequest();
            request.setEmail("john@example.com");
            request.setOtp("123456");

            Set<ConstraintViolation<EmailVerificationRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should reject non-6-digit or non-numeric OTP")
        void shouldRejectInvalidOtp() {
            EmailVerificationRequest request = new EmailVerificationRequest();
            request.setEmail("john@example.com");
            request.setOtp("12345"); // 5 digits

            Set<ConstraintViolation<EmailVerificationRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("otp")));

            request.setOtp("12345A"); // non-numeric
            violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("otp")));
        }
    }

    @Nested
    @DisplayName("CreateAuctionRequest Validation")
    class CreateAuctionRequestValidation {

        @Test
        @DisplayName("Should accept valid create auction request")
        void shouldAcceptValidRequest() {
            CreateAuctionRequest request = new CreateAuctionRequest();
            request.setCraftId(10L);
            request.setStartingPrice(new BigDecimal("100.00"));
            request.setReservePrice(new BigDecimal("150.00"));
            request.setMinBidIncrement(new BigDecimal("5.00"));
            request.setDurationHours(24);

            Set<ConstraintViolation<CreateAuctionRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should reject invalid prices and duration")
        void shouldRejectInvalidFields() {
            CreateAuctionRequest request = new CreateAuctionRequest();
            request.setCraftId(-1L); // negative
            request.setStartingPrice(new BigDecimal("0.50")); // < 1.00
            request.setDurationHours(1000); // > 720 hours

            Set<ConstraintViolation<CreateAuctionRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("craftId")));
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("startingPrice")));
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("durationHours")));
        }
    }

    @Nested
    @DisplayName("PlaceBidRequest Validation")
    class PlaceBidRequestValidation {

        @Test
        @DisplayName("Should accept valid bid amount")
        void shouldAcceptValidBid() {
            PlaceBidRequest request = new PlaceBidRequest();
            request.setAmount(new BigDecimal("250.00"));

            Set<ConstraintViolation<PlaceBidRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should reject non-positive or null bid amount")
        void shouldRejectInvalidBid() {
            PlaceBidRequest request = new PlaceBidRequest();
            request.setAmount(new BigDecimal("-10.00"));

            Set<ConstraintViolation<PlaceBidRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
        }
    }

    @Nested
    @DisplayName("SubmitAddressRequest Validation")
    class SubmitAddressRequestValidation {

        @Test
        @DisplayName("Should accept valid delivery address")
        void shouldAcceptValidAddress() {
            SubmitAddressRequest request = new SubmitAddressRequest();
            request.setFullName("Jane Doe");
            request.setPhone("9876543210");
            request.setStreetAddress("123 Main Street");
            request.setCity("Mumbai");
            request.setState("Maharashtra");
            request.setPincode("400001");

            Set<ConstraintViolation<SubmitAddressRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should reject invalid 5-digit or alphanumeric pincode")
        void shouldRejectInvalidPincode() {
            SubmitAddressRequest request = new SubmitAddressRequest();
            request.setFullName("Jane Doe");
            request.setPhone("9876543210");
            request.setStreetAddress("123 Main Street");
            request.setCity("Mumbai");
            request.setState("Maharashtra");
            request.setPincode("4000A1"); // invalid format

            Set<ConstraintViolation<SubmitAddressRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("pincode")));
        }
    }

    @Nested
    @DisplayName("JoinAuctionRequest Validation")
    class JoinAuctionRequestValidation {

        @Test
        @DisplayName("Should accept valid payment method enum")
        void shouldAcceptValidMethod() {
            JoinAuctionRequest request = new JoinAuctionRequest();
            request.setPaymentMethod("UPI");
            request.setTransactionRef("TXN_123456");

            Set<ConstraintViolation<JoinAuctionRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should reject invalid payment method")
        void shouldRejectInvalidMethod() {
            JoinAuctionRequest request = new JoinAuctionRequest();
            request.setPaymentMethod("CRYPTO_DOGE");

            Set<ConstraintViolation<JoinAuctionRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("paymentMethod")));
        }
    }

    @Nested
    @DisplayName("SupportTicketRequest Validation")
    class SupportTicketRequestValidation {

        @Test
        @DisplayName("Should accept valid support ticket request")
        void shouldAcceptValidTicket() {
            SupportTicketRequest request = new SupportTicketRequest();
            request.setName("Alice");
            request.setEmail("alice@example.com");
            request.setCategory("ORDER");
            request.setSubject("Issue with my recent delivery");
            request.setMessage("I have not received tracking updates for 3 days.");

            Set<ConstraintViolation<SupportTicketRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should reject ticket with blank subject or invalid email")
        void shouldRejectInvalidTicket() {
            SupportTicketRequest request = new SupportTicketRequest();
            request.setName("A");
            request.setEmail("invalid-email");
            request.setCategory("");
            request.setSubject("");
            request.setMessage("Hi"); // too short

            Set<ConstraintViolation<SupportTicketRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("subject")));
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("message")));
        }
    }
}
