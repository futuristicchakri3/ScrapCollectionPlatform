package com.scrapconnection.controller;

import com.scrapconnection.dto.SettlementDto;
import com.scrapconnection.dto.SettlementItemDto;
import com.scrapconnection.entity.*;
import com.scrapconnection.enums.ApprovalStatus;
import com.scrapconnection.enums.BookingStatus;
import com.scrapconnection.exception.UnauthorizedException;
import com.scrapconnection.repository.MaterialPriceRepository;
import com.scrapconnection.repository.ScrapBookingRepository;
import com.scrapconnection.repository.SettlementRepository;
import com.scrapconnection.security.SecurityUtils;
import com.scrapconnection.service.BookingService;
import com.scrapconnection.service.CouponService;
import com.scrapconnection.service.SettlementService;
import com.scrapconnection.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/shopkeeper")
public class ShopkeeperController {

    private final UserService userService;
    private final BookingService bookingService;
    private final SettlementService settlementService;
    private final CouponService couponService;
    private final ScrapBookingRepository bookingRepository;
    private final SettlementRepository settlementRepository;
    private final MaterialPriceRepository priceRepository;

    public ShopkeeperController(UserService userService,
                                BookingService bookingService,
                                SettlementService settlementService,
                                CouponService couponService,
                                ScrapBookingRepository bookingRepository,
                                SettlementRepository settlementRepository,
                                MaterialPriceRepository priceRepository) {
        this.userService = userService;
        this.bookingService = bookingService;
        this.settlementService = settlementService;
        this.couponService = couponService;
        this.bookingRepository = bookingRepository;
        this.settlementRepository = settlementRepository;
        this.priceRepository = priceRepository;
    }

    private ShopkeeperProfile getAuthenticatedShopkeeper() {
        var userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            throw new UnauthorizedException("User not authenticated.");
        }
        return userService.getShopkeeperProfile(userDetails.getUser());
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        ShopkeeperProfile shopkeeper = getAuthenticatedShopkeeper();

        long acceptedCount = bookingRepository.countByAssignedShopkeeperAndStatus(shopkeeper, BookingStatus.ACCEPTED);
        long inProgressCount = bookingRepository.countByAssignedShopkeeperAndStatus(shopkeeper, BookingStatus.PICKUP_IN_PROGRESS);
        long completedCount = bookingRepository.countByAssignedShopkeeperAndStatus(shopkeeper, BookingStatus.COMPLETED);
        BigDecimal totalEarnings = settlementRepository.sumSettledAmountByShopkeeper(shopkeeper);

        List<ScrapBooking> todayPickups = bookingService.getShopkeeperTodayBookings(shopkeeper);
        List<ScrapBooking> availableRequests = bookingService.getAvailableBookingsForShopkeeper(shopkeeper);

        model.addAttribute("shopkeeper", shopkeeper);
        model.addAttribute("acceptedCount", acceptedCount);
        model.addAttribute("inProgressCount", inProgressCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("totalEarnings", totalEarnings != null ? totalEarnings : BigDecimal.ZERO);
        model.addAttribute("todayPickups", todayPickups);
        model.addAttribute("availableRequests", availableRequests.stream().limit(5).toList());

        return "shopkeeper/dashboard";
    }

    @GetMapping("/requests")
    public String availableRequests(Model model) {
        ShopkeeperProfile shopkeeper = getAuthenticatedShopkeeper();
        List<ScrapBooking> requests = bookingService.getAvailableBookingsForShopkeeper(shopkeeper);

        model.addAttribute("shopkeeper", shopkeeper);
        model.addAttribute("requests", requests);
        return "shopkeeper/requests";
    }

    @PostMapping("/requests/{id}/accept")
    public String acceptRequest(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        ShopkeeperProfile shopkeeper = getAuthenticatedShopkeeper();
        try {
            ScrapBooking booking = bookingService.acceptBooking(id, shopkeeper);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Booking " + booking.getBookingReference() + " accepted! It is now scheduled in your pickups.");
            return "redirect:/shopkeeper/pickups";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/shopkeeper/requests";
        }
    }

    @GetMapping("/pickups")
    public String myPickups(Model model) {
        ShopkeeperProfile shopkeeper = getAuthenticatedShopkeeper();
        List<ScrapBooking> allAssigned = bookingService.getShopkeeperBookings(shopkeeper);

        List<ScrapBooking> activePickups = allAssigned.stream()
                .filter(b -> b.getStatus() == BookingStatus.ACCEPTED || b.getStatus() == BookingStatus.PICKUP_IN_PROGRESS)
                .toList();

        model.addAttribute("shopkeeper", shopkeeper);
        model.addAttribute("pickups", activePickups);
        return "shopkeeper/pickups";
    }

    @PostMapping("/pickups/{id}/start")
    public String startPickup(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        ShopkeeperProfile shopkeeper = getAuthenticatedShopkeeper();
        try {
            bookingService.startPickup(id, shopkeeper);
            redirectAttributes.addFlashAttribute("successMessage", "Pickup marked as in progress.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/shopkeeper/pickups";
    }

    @GetMapping("/pickups/{id}/weigh")
    public String weighPage(@PathVariable("id") Long id, Model model) {
        ShopkeeperProfile shopkeeper = getAuthenticatedShopkeeper();
        ScrapBooking booking = bookingService.getBookingByIdAndShopkeeper(id, shopkeeper);

        SettlementDto settlementDto = new SettlementDto();
        settlementDto.setBookingId(id);
        settlementDto.setSettlementMethod("CASH");

        List<SettlementItemDto> items = new ArrayList<>();
        for (BookingMaterial bm : booking.getMaterials()) {
            MaterialPrice activePrice = priceRepository.findByCategoryAndActiveTrue(bm.getCategory())
                    .orElse(null);
            BigDecimal rate = activePrice != null ? activePrice.getPricePerKg() : BigDecimal.valueOf(15.0);

            SettlementItemDto itemDto = new SettlementItemDto(
                    bm.getId(),
                    bm.getCategory().getId(),
                    bm.getCategory().getDisplayName(),
                    bm.getEstimatedWeightKg(),
                    rate
            );
            items.add(itemDto);
        }
        settlementDto.setItems(items);

        model.addAttribute("shopkeeper", shopkeeper);
        model.addAttribute("booking", booking);
        model.addAttribute("settlementDto", settlementDto);

        return "shopkeeper/weigh";
    }

    @PostMapping("/pickups/{id}/settle")
    public String processSettlement(@PathVariable("id") Long id,
                                    @ModelAttribute("settlementDto") SettlementDto dto,
                                    RedirectAttributes redirectAttributes) {
        ShopkeeperProfile shopkeeper = getAuthenticatedShopkeeper();
        try {
            Settlement settlement = settlementService.processSettlement(id, shopkeeper, dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Pickup settled successfully! Total amount: ₹" + settlement.getNetAmount() +
                    " settled via " + settlement.getSettlementMethod());
            return "redirect:/shopkeeper/history";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/shopkeeper/pickups/" + id + "/weigh";
        }
    }

    @GetMapping("/history")
    public String history(Model model) {
        ShopkeeperProfile shopkeeper = getAuthenticatedShopkeeper();
        List<Settlement> settlements = settlementService.getShopkeeperSettlements(shopkeeper);

        model.addAttribute("shopkeeper", shopkeeper);
        model.addAttribute("settlements", settlements);
        return "shopkeeper/history";
    }
}
