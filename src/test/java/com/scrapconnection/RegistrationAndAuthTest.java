package com.scrapconnection;

import com.scrapconnection.dto.CustomerRegistrationDto;
import com.scrapconnection.entity.User;
import com.scrapconnection.enums.AccountStatus;
import com.scrapconnection.enums.Role;
import com.scrapconnection.exception.BadRequestException;
import com.scrapconnection.repository.UserRepository;
import com.scrapconnection.service.OtpService;
import com.scrapconnection.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class RegistrationAndAuthTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpService otpService;

    @Test
    @DisplayName("Should successfully register customer in PENDING_VERIFICATION state")
    void testCustomerRegistrationSuccess() {
        CustomerRegistrationDto dto = new CustomerRegistrationDto();
        dto.setFullName("Pooja Nair");
        dto.setMobileNumber("9849012345");
        dto.setEmail("pooja@example.com");
        dto.setPassword("Secret@123");
        dto.setConfirmPassword("Secret@123");
        dto.setAddress("H.No 12-4, Banjara Hills");
        dto.setLocality("Banjara Hills");
        dto.setTermsAccepted(true);

        User user = userService.registerCustomer(dto);

        assertNotNull(user.getId());
        assertEquals("9849012345", user.getMobileNumber());
        assertEquals(Role.ROLE_CUSTOMER, user.getRole());
        assertEquals(AccountStatus.PENDING_VERIFICATION, user.getStatus());
        assertTrue(userRepository.existsByMobileNumber("9849012345"));
    }

    @Test
    @DisplayName("Should reject customer registration when password and confirm password mismatch")
    void testPasswordMismatch() {
        CustomerRegistrationDto dto = new CustomerRegistrationDto();
        dto.setFullName("Arun Verma");
        dto.setMobileNumber("9871112233");
        dto.setPassword("Secret@123");
        dto.setConfirmPassword("Wrong@123");
        dto.setAddress("Test Address");
        dto.setLocality("Test Locality");
        dto.setTermsAccepted(true);

        assertThrows(BadRequestException.class, () -> userService.registerCustomer(dto));
    }

    @Test
    @DisplayName("Should reject duplicate mobile number registration")
    void testDuplicateMobileRegistration() {
        CustomerRegistrationDto dto1 = new CustomerRegistrationDto();
        dto1.setFullName("User One");
        dto1.setMobileNumber("9700011122");
        dto1.setPassword("Pass@123");
        dto1.setConfirmPassword("Pass@123");
        dto1.setAddress("Addr 1");
        dto1.setLocality("Loc 1");
        dto1.setTermsAccepted(true);
        userService.registerCustomer(dto1);

        CustomerRegistrationDto dto2 = new CustomerRegistrationDto();
        dto2.setFullName("User Two");
        dto2.setMobileNumber("9700011122"); // Same mobile
        dto2.setPassword("Pass@456");
        dto2.setConfirmPassword("Pass@456");
        dto2.setAddress("Addr 2");
        dto2.setLocality("Loc 2");
        dto2.setTermsAccepted(true);

        assertThrows(BadRequestException.class, () -> userService.registerCustomer(dto2));
    }

    @Test
    @DisplayName("Should successfully verify OTP and activate customer account")
    void testOtpVerificationSuccess() {
        CustomerRegistrationDto dto = new CustomerRegistrationDto();
        dto.setFullName("Kavita Reddy");
        dto.setMobileNumber("9948011223");
        dto.setPassword("Pass@123");
        dto.setConfirmPassword("Pass@123");
        dto.setAddress("Jubilee Hills");
        dto.setLocality("Jubilee Hills");
        dto.setTermsAccepted(true);
        userService.registerCustomer(dto);

        String otp = otpService.getLatestDevOtp("9948011223", "REGISTRATION");
        assertNotNull(otp);

        userService.verifyCustomerOtp("9948011223", otp);

        User updatedUser = userRepository.findByMobileNumber("9948011223").orElseThrow();
        assertEquals(AccountStatus.ACTIVE, updatedUser.getStatus());
    }

    @Test
    @DisplayName("Should reject invalid OTP code")
    void testInvalidOtpRejection() {
        CustomerRegistrationDto dto = new CustomerRegistrationDto();
        dto.setFullName("Vikram Sen");
        dto.setMobileNumber("9652033445");
        dto.setPassword("Pass@123");
        dto.setConfirmPassword("Pass@123");
        dto.setAddress("Kondapur");
        dto.setLocality("Kondapur");
        dto.setTermsAccepted(true);
        userService.registerCustomer(dto);

        assertThrows(BadRequestException.class, () -> {
            userService.verifyCustomerOtp("9652033445", "000000"); // Wrong OTP
        });
    }
}
