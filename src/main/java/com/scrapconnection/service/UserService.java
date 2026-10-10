package com.scrapconnection.service;

import com.scrapconnection.dto.CustomerRegistrationDto;
import com.scrapconnection.dto.ShopkeeperRegistrationDto;
import com.scrapconnection.entity.Address;
import com.scrapconnection.entity.CustomerProfile;
import com.scrapconnection.entity.ShopkeeperProfile;
import com.scrapconnection.entity.User;
import com.scrapconnection.enums.AccountStatus;
import com.scrapconnection.enums.ApprovalStatus;
import com.scrapconnection.enums.Role;
import com.scrapconnection.exception.BadRequestException;
import com.scrapconnection.exception.ResourceNotFoundException;
import com.scrapconnection.repository.AddressRepository;
import com.scrapconnection.repository.CustomerProfileRepository;
import com.scrapconnection.repository.ShopkeeperProfileRepository;
import com.scrapconnection.repository.UserRepository;
import com.scrapconnection.security.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ShopkeeperProfileRepository shopkeeperProfileRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final LoyaltyService loyaltyService;

    public UserService(UserRepository userRepository,
                       CustomerProfileRepository customerProfileRepository,
                       ShopkeeperProfileRepository shopkeeperProfileRepository,
                       AddressRepository addressRepository,
                       PasswordEncoder passwordEncoder,
                       OtpService otpService,
                       LoyaltyService loyaltyService) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.shopkeeperProfileRepository = shopkeeperProfileRepository;
        this.addressRepository = addressRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.loyaltyService = loyaltyService;
    }

    @Transactional
    public User registerCustomer(CustomerRegistrationDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BadRequestException("Password and confirm password do not match.");
        }

        if (userRepository.existsByMobileNumber(dto.getMobileNumber())) {
            throw new BadRequestException("An account with mobile number " + dto.getMobileNumber() + " already exists.");
        }

        // 1. Create User in PENDING_VERIFICATION state
        User user = new User(
                dto.getMobileNumber(),
                passwordEncoder.encode(dto.getPassword()),
                Role.ROLE_CUSTOMER,
                AccountStatus.PENDING_VERIFICATION
        );
        User savedUser = userRepository.save(user);

        // 2. Create Customer Profile
        CustomerProfile profile = new CustomerProfile(
                savedUser,
                dto.getFullName(),
                dto.getEmail(),
                dto.getLocality(),
                dto.getAddress()
        );
        CustomerProfile savedProfile = customerProfileRepository.save(profile);

        // 3. Create Default Address
        Address address = new Address(
                savedProfile,
                "Home",
                dto.getAddress(),
                dto.getLocality(),
                "City",
                "500001",
                dto.getMobileNumber(),
                true
        );
        addressRepository.save(address);

        // 4. Send Registration OTP
        otpService.generateAndSendOtp(dto.getMobileNumber(), "REGISTRATION");

        return savedUser;
    }

    @Transactional
    public void verifyCustomerOtp(String mobileNumber, String otpCode) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for mobile: " + mobileNumber));

        if (user.getStatus() == AccountStatus.ACTIVE) {
            return; // Already verified
        }

        otpService.verifyOtp(mobileNumber, otpCode, "REGISTRATION");

        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        // Initialize customer's loyalty wallet
        CustomerProfile profile = customerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
        loyaltyService.getOrCreateAccount(profile);
    }

    @Transactional
    public void resendRegistrationOtp(String mobileNumber) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for mobile: " + mobileNumber));

        if (user.getStatus() == AccountStatus.ACTIVE) {
            throw new BadRequestException("Account is already verified. Please log in.");
        }

        otpService.generateAndSendOtp(mobileNumber, "REGISTRATION");
    }

    @Transactional
    public User registerShopkeeper(ShopkeeperRegistrationDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BadRequestException("Password and confirm password do not match.");
        }

        if (userRepository.existsByMobileNumber(dto.getMobileNumber())) {
            throw new BadRequestException("An account with mobile number " + dto.getMobileNumber() + " already exists.");
        }

        // Shopkeeper accounts start as PENDING_APPROVAL
        User user = new User(
                dto.getMobileNumber(),
                passwordEncoder.encode(dto.getPassword()),
                Role.ROLE_SHOPKEEPER,
                AccountStatus.PENDING_APPROVAL
        );
        User savedUser = userRepository.save(user);

        ShopkeeperProfile profile = new ShopkeeperProfile(
                savedUser,
                dto.getOwnerName(),
                dto.getShopName(),
                dto.getShopAddress(),
                dto.getLocality(),
                dto.getCity(),
                dto.getPostalCode(),
                dto.getEmail(),
                dto.getShopDescription(),
                dto.getSupportedCategories() != null && !dto.getSupportedCategories().isBlank() ? dto.getSupportedCategories() : "ALL"
        );
        profile.setApprovalStatus(ApprovalStatus.PENDING);
        shopkeeperProfileRepository.save(profile);

        return savedUser;
    }

    @Transactional
    public void authenticateWithOtp(String mobileNumber, String otpCode) {
        otpService.verifyOtp(mobileNumber, otpCode, "LOGIN");

        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with mobile: " + mobileNumber));

        if (user.getStatus() == AccountStatus.PENDING_VERIFICATION) {
            user.setStatus(AccountStatus.ACTIVE);
            userRepository.save(user);
        } else if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw new BadRequestException("Your account is currently suspended. Please contact administrator.");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public void sendLoginOtp(String mobileNumber) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("No account registered with mobile number: " + mobileNumber));

        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw new BadRequestException("Account is suspended.");
        }

        otpService.generateAndSendOtp(mobileNumber, "LOGIN");
    }

    @Transactional(readOnly = true)
    public CustomerProfile getCustomerProfile(User user) {
        return customerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + user.getMobileNumber()));
    }

    @Transactional(readOnly = true)
    public ShopkeeperProfile getShopkeeperProfile(User user) {
        return shopkeeperProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Shopkeeper profile not found for user: " + user.getMobileNumber()));
    }

    @Transactional(readOnly = true)
    public Optional<User> findByMobileNumber(String mobileNumber) {
        return userRepository.findByMobileNumber(mobileNumber);
    }
}
