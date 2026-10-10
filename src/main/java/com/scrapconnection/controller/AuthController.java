package com.scrapconnection.controller;

import com.scrapconnection.dto.CustomerRegistrationDto;
import com.scrapconnection.dto.LoginDto;
import com.scrapconnection.dto.OtpVerificationDto;
import com.scrapconnection.dto.ShopkeeperRegistrationDto;
import com.scrapconnection.entity.User;
import com.scrapconnection.exception.BadRequestException;
import com.scrapconnection.security.SecurityUtils;
import com.scrapconnection.service.OtpService;
import com.scrapconnection.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final OtpService otpService;

    public AuthController(UserService userService, OtpService otpService) {
        this.userService = userService;
        this.otpService = otpService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "registeredShopkeeper", required = false) String registeredShopkeeper,
                            @RequestParam(value = "verified", required = false) String verified,
                            Model model) {
        if (SecurityUtils.isAuthenticated()) {
            return redirectLoggedInUser();
        }

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid mobile number or password. Please check your credentials.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        if (registeredShopkeeper != null) {
            model.addAttribute("successMessage", "Registration submitted successfully! Your shop account is pending administrator review and approval.");
        }
        if (verified != null) {
            model.addAttribute("successMessage", "Mobile verification successful! Please log in with your credentials.");
        }

        if (!model.containsAttribute("loginDto")) {
            model.addAttribute("loginDto", new LoginDto());
        }
        model.addAttribute("isDevMode", otpService.isDevelopmentMode());
        return "auth/login";
    }

    @GetMapping("/register/customer")
    public String registerCustomerPage(Model model) {
        if (SecurityUtils.isAuthenticated()) {
            return redirectLoggedInUser();
        }
        if (!model.containsAttribute("customerDto")) {
            model.addAttribute("customerDto", new CustomerRegistrationDto());
        }
        return "auth/register-customer";
    }

    @PostMapping("/register/customer")
    public String processCustomerRegistration(@Valid @ModelAttribute("customerDto") CustomerRegistrationDto dto,
                                              BindingResult bindingResult,
                                              RedirectAttributes redirectAttributes,
                                              Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register-customer";
        }

        try {
            userService.registerCustomer(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Registration initiated! Please verify your mobile number with the OTP.");
            return "redirect:/verify-otp?mobile=" + dto.getMobileNumber() + "&purpose=REGISTRATION";
        } catch (BadRequestException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register-customer";
        }
    }

    @GetMapping("/register/shopkeeper")
    public String registerShopkeeperPage(Model model) {
        if (SecurityUtils.isAuthenticated()) {
            return redirectLoggedInUser();
        }
        if (!model.containsAttribute("shopkeeperDto")) {
            model.addAttribute("shopkeeperDto", new ShopkeeperRegistrationDto());
        }
        return "auth/register-shopkeeper";
    }

    @PostMapping("/register/shopkeeper")
    public String processShopkeeperRegistration(@Valid @ModelAttribute("shopkeeperDto") ShopkeeperRegistrationDto dto,
                                                BindingResult bindingResult,
                                                RedirectAttributes redirectAttributes,
                                                Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register-shopkeeper";
        }

        try {
            userService.registerShopkeeper(dto);
            return "redirect:/login?registeredShopkeeper=true";
        } catch (BadRequestException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register-shopkeeper";
        }
    }

    @GetMapping("/verify-otp")
    public String verifyOtpPage(@RequestParam("mobile") String mobileNumber,
                                @RequestParam(value = "purpose", defaultValue = "REGISTRATION") String purpose,
                                @RequestParam(value = "resent", required = false) String resent,
                                Model model) {
        OtpVerificationDto dto = new OtpVerificationDto(mobileNumber, "", purpose);
        model.addAttribute("otpDto", dto);
        model.addAttribute("mobileNumber", mobileNumber);
        model.addAttribute("purpose", purpose);
        model.addAttribute("isDevMode", otpService.isDevelopmentMode());

        if (resent != null) {
            model.addAttribute("successMessage", "A new OTP has been generated successfully.");
        }

        if (otpService.isDevelopmentMode()) {
            String devOtp = otpService.getLatestDevOtp(mobileNumber, purpose);
            model.addAttribute("devOtp", devOtp);
        }

        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String processOtpVerification(@Valid @ModelAttribute("otpDto") OtpVerificationDto dto,
                                         BindingResult bindingResult,
                                         RedirectAttributes redirectAttributes,
                                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mobileNumber", dto.getMobileNumber());
            model.addAttribute("purpose", dto.getPurpose());
            model.addAttribute("isDevMode", otpService.isDevelopmentMode());
            if (otpService.isDevelopmentMode()) {
                model.addAttribute("devOtp", otpService.getLatestDevOtp(dto.getMobileNumber(), dto.getPurpose()));
            }
            return "auth/verify-otp";
        }

        try {
            if ("LOGIN".equalsIgnoreCase(dto.getPurpose())) {
                userService.authenticateWithOtp(dto.getMobileNumber(), dto.getOtp());
                return redirectLoggedInUser();
            } else {
                userService.verifyCustomerOtp(dto.getMobileNumber(), dto.getOtp());
                return "redirect:/login?verified=true";
            }
        } catch (BadRequestException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("mobileNumber", dto.getMobileNumber());
            model.addAttribute("purpose", dto.getPurpose());
            model.addAttribute("isDevMode", otpService.isDevelopmentMode());
            if (otpService.isDevelopmentMode()) {
                model.addAttribute("devOtp", otpService.getLatestDevOtp(dto.getMobileNumber(), dto.getPurpose()));
            }
            return "auth/verify-otp";
        }
    }

    @PostMapping("/resend-otp")
    public String resendOtp(@RequestParam("mobileNumber") String mobileNumber,
                            @RequestParam(value = "purpose", defaultValue = "REGISTRATION") String purpose,
                            RedirectAttributes redirectAttributes) {
        try {
            if ("LOGIN".equalsIgnoreCase(purpose)) {
                userService.sendLoginOtp(mobileNumber);
            } else {
                userService.resendRegistrationOtp(mobileNumber);
            }
            return "redirect:/verify-otp?mobile=" + mobileNumber + "&purpose=" + purpose + "&resent=true";
        } catch (BadRequestException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/verify-otp?mobile=" + mobileNumber + "&purpose=" + purpose;
        }
    }

    @PostMapping("/send-login-otp")
    public String sendLoginOtp(@RequestParam("mobileNumber") String mobileNumber,
                               RedirectAttributes redirectAttributes) {
        try {
            userService.sendLoginOtp(mobileNumber);
            return "redirect:/verify-otp?mobile=" + mobileNumber + "&purpose=LOGIN";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/login";
        }
    }

    private String redirectLoggedInUser() {
        var userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails != null) {
            String role = userDetails.getRoleName();
            if ("ROLE_ADMIN".equals(role)) {
                return "redirect:/admin/dashboard";
            } else if ("ROLE_SHOPKEEPER".equals(role)) {
                return "redirect:/shopkeeper/dashboard";
            } else if ("ROLE_CUSTOMER".equals(role)) {
                return "redirect:/customer/dashboard";
            }
        }
        return "redirect:/";
    }
}
