package com.scrapconnection.controller;

import com.scrapconnection.dto.AddressDto;
import com.scrapconnection.dto.ScrapBookingDto;
import com.scrapconnection.entity.*;
import com.scrapconnection.enums.BookingStatus;
import com.scrapconnection.exception.BadRequestException;
import com.scrapconnection.exception.UnauthorizedException;
import com.scrapconnection.repository.*;
import com.scrapconnection.security.SecurityUtils;
import com.scrapconnection.service.BookingService;
import com.scrapconnection.service.CouponService;
import com.scrapconnection.service.LoyaltyService;
import com.scrapconnection.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    private final UserService userService;
    private final BookingService bookingService;
    private final CouponService couponService;
    private final LoyaltyService loyaltyService;
    private final ScrapBookingRepository bookingRepository;
    private final MaterialCategoryRepository categoryRepository;
    private final MaterialPriceRepository priceRepository;
    private final AddressRepository addressRepository;
    private final SettlementRepository settlementRepository;

    public CustomerController(UserService userService,
                              BookingService bookingService,
                              CouponService couponService,
                              LoyaltyService loyaltyService,
                              ScrapBookingRepository bookingRepository,
                              MaterialCategoryRepository categoryRepository,
                              MaterialPriceRepository priceRepository,
                              AddressRepository addressRepository,
                              SettlementRepository settlementRepository) {
        this.userService = userService;
        this.bookingService = bookingService;
        this.couponService = couponService;
        this.loyaltyService = loyaltyService;
        this.bookingRepository = bookingRepository;
        this.categoryRepository = categoryRepository;
        this.priceRepository = priceRepository;
        this.addressRepository = addressRepository;
        this.settlementRepository = settlementRepository;
    }

    private CustomerProfile getAuthenticatedCustomer() {
        var userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            throw new UnauthorizedException("User not authenticated.");
        }
        return userService.getCustomerProfile(userDetails.getUser());
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        CustomerProfile customer = getAuthenticatedCustomer();

        // 1. KPI Statistics from actual database values
        long totalBookings = bookingRepository.countByCustomerProfile(customer);
        long completedCount = bookingRepository.countByCustomerProfileAndStatus(customer, BookingStatus.COMPLETED);
        long upcomingCount = bookingRepository.countByCustomerProfileAndStatus(customer, BookingStatus.REQUESTED)
                + bookingRepository.countByCustomerProfileAndStatus(customer, BookingStatus.ACCEPTED);
        long inProgressCount = bookingRepository.countByCustomerProfileAndStatus(customer, BookingStatus.PICKUP_IN_PROGRESS);
        long cancelledCount = bookingRepository.countByCustomerProfileAndStatus(customer, BookingStatus.CANCELLED);

        BigDecimal totalAmountReceived = bookingRepository.sumTotalAmountByCustomerCompleted(customer);
        BigDecimal totalWeightSold = bookingRepository.sumTotalWeightByCustomerCompleted(customer);
        BigDecimal couponBalance = couponService.getAvailableBalance(customer);
        int loyaltyPoints = loyaltyService.getAvailablePoints(customer);

        List<ScrapBooking> recentBookings = bookingRepository.findByCustomerProfileOrderByCreatedAtDesc(customer);

        model.addAttribute("customer", customer);
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("upcomingCount", upcomingCount);
        model.addAttribute("inProgressCount", inProgressCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("totalAmountReceived", totalAmountReceived != null ? totalAmountReceived : BigDecimal.ZERO);
        model.addAttribute("totalWeightSold", totalWeightSold != null ? totalWeightSold : BigDecimal.ZERO);
        model.addAttribute("couponBalance", couponBalance);
        model.addAttribute("loyaltyPoints", loyaltyPoints);
        model.addAttribute("recentBookings", recentBookings.stream().limit(5).toList());

        return "customer/dashboard";
    }

    @GetMapping("/book-scrap")
    public String bookScrapPage(Model model) {
        CustomerProfile customer = getAuthenticatedCustomer();

        ScrapBookingDto dto = new ScrapBookingDto();
        dto.setContactNumber(customer.getUser().getMobileNumber());
        dto.setPickupAddress(customer.getDefaultAddress());
        dto.setLocality(customer.getDefaultLocality());
        dto.setPreferredDate(LocalDate.now().plusDays(1));
        dto.setTimeSlot("10:00 AM - 01:00 PM");

        List<Address> savedAddresses = addressRepository.findByCustomerProfile(customer);

        model.addAttribute("bookingDto", dto);
        model.addAttribute("customer", customer);
        model.addAttribute("categories", categoryRepository.findByActiveTrue());
        model.addAttribute("materialPrices", priceRepository.findByActiveTrue());
        model.addAttribute("savedAddresses", savedAddresses);

        return "customer/book-scrap";
    }

    @PostMapping("/book-scrap")
    public String processBooking(@Valid @ModelAttribute("bookingDto") ScrapBookingDto dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        CustomerProfile customer = getAuthenticatedCustomer();

        if (bindingResult.hasErrors()) {
            model.addAttribute("customer", customer);
            model.addAttribute("categories", categoryRepository.findByActiveTrue());
            model.addAttribute("materialPrices", priceRepository.findByActiveTrue());
            model.addAttribute("savedAddresses", addressRepository.findByCustomerProfile(customer));
            return "customer/book-scrap";
        }

        try {
            ScrapBooking booking = bookingService.createBooking(customer, dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Pickup scheduled successfully! Reference ID: " + booking.getBookingReference());
            return "redirect:/customer/bookings/" + booking.getId();
        } catch (BadRequestException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("customer", customer);
            model.addAttribute("categories", categoryRepository.findByActiveTrue());
            model.addAttribute("materialPrices", priceRepository.findByActiveTrue());
            model.addAttribute("savedAddresses", addressRepository.findByCustomerProfile(customer));
            return "customer/book-scrap";
        }
    }

    @GetMapping("/bookings")
    public String bookingsList(@RequestParam(value = "status", required = false) String statusFilter, Model model) {
        CustomerProfile customer = getAuthenticatedCustomer();
        List<ScrapBooking> bookings;

        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                BookingStatus status = BookingStatus.valueOf(statusFilter.toUpperCase());
                bookings = bookingRepository.findByCustomerProfileAndStatusOrderByCreatedAtDesc(customer, status);
            } catch (Exception e) {
                bookings = bookingRepository.findByCustomerProfileOrderByCreatedAtDesc(customer);
            }
        } else {
            bookings = bookingRepository.findByCustomerProfileOrderByCreatedAtDesc(customer);
        }

        model.addAttribute("customer", customer);
        model.addAttribute("bookings", bookings);
        model.addAttribute("selectedStatus", statusFilter);
        return "customer/bookings";
    }

    @GetMapping("/bookings/{id}")
    public String bookingDetails(@PathVariable("id") Long id, Model model) {
        CustomerProfile customer = getAuthenticatedCustomer();
        ScrapBooking booking = bookingService.getBookingByIdAndCustomer(id, customer);

        model.addAttribute("customer", customer);
        model.addAttribute("booking", booking);
        model.addAttribute("statusHistories", booking.getStatusHistories());
        model.addAttribute("settlement", settlementRepository.findByBooking(booking).orElse(null));

        return "customer/booking-details";
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancelBooking(@PathVariable("id") Long id,
                                @RequestParam(value = "reason", required = false) String reason,
                                RedirectAttributes redirectAttributes) {
        CustomerProfile customer = getAuthenticatedCustomer();
        try {
            bookingService.cancelBooking(id, customer, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Booking has been cancelled.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/bookings/" + id;
    }

    @GetMapping("/coupons")
    public String couponWallet(Model model) {
        CustomerProfile customer = getAuthenticatedCustomer();

        model.addAttribute("customer", customer);
        model.addAttribute("availableBalance", couponService.getAvailableBalance(customer));
        model.addAttribute("coupons", couponService.getCustomerCoupons(customer));
        model.addAttribute("ledgers", couponService.getCustomerLedger(customer));

        return "customer/coupons";
    }

    @GetMapping("/loyalty")
    public String loyaltyPoints(Model model) {
        CustomerProfile customer = getAuthenticatedCustomer();

        model.addAttribute("customer", customer);
        model.addAttribute("totalPoints", loyaltyService.getAvailablePoints(customer));
        model.addAttribute("transactions", loyaltyService.getTransactionHistory(customer));

        return "customer/loyalty";
    }

    @GetMapping("/profile")
    public String profilePage(Model model) {
        CustomerProfile customer = getAuthenticatedCustomer();
        model.addAttribute("customer", customer);
        model.addAttribute("addresses", addressRepository.findByCustomerProfile(customer));
        model.addAttribute("addressDto", new AddressDto());
        return "customer/profile";
    }

    @PostMapping("/address")
    public String addAddress(@Valid @ModelAttribute("addressDto") AddressDto dto,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        CustomerProfile customer = getAuthenticatedCustomer();
        if (bindingResult.hasErrors()) {
            model.addAttribute("customer", customer);
            model.addAttribute("addresses", addressRepository.findByCustomerProfile(customer));
            return "customer/profile";
        }

        Address address = new Address(
                customer,
                dto.getTitle() != null && !dto.getTitle().isBlank() ? dto.getTitle() : "Home",
                dto.getStreetAddress(),
                dto.getLocality(),
                dto.getCity(),
                dto.getPostalCode(),
                dto.getContactNumber() != null && !dto.getContactNumber().isBlank() ? dto.getContactNumber() : customer.getUser().getMobileNumber(),
                dto.isDefault()
        );
        addressRepository.save(address);

        redirectAttributes.addFlashAttribute("successMessage", "New pickup address added successfully.");
        return "redirect:/customer/profile";
    }
}
