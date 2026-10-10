package com.scrapconnection;

import com.scrapconnection.dto.ScrapBookingDto;
import com.scrapconnection.dto.SettlementDto;
import com.scrapconnection.dto.SettlementItemDto;
import com.scrapconnection.entity.*;
import com.scrapconnection.enums.BookingStatus;
import com.scrapconnection.enums.SettlementMethod;
import com.scrapconnection.exception.ConflictException;
import com.scrapconnection.repository.CustomerProfileRepository;
import com.scrapconnection.repository.ShopkeeperProfileRepository;
import com.scrapconnection.service.BookingService;
import com.scrapconnection.service.CouponService;
import com.scrapconnection.service.LoyaltyService;
import com.scrapconnection.service.SettlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ScrapSettlementAndLoyaltyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private LoyaltyService loyaltyService;

    @Autowired
    private CustomerProfileRepository customerRepository;

    @Autowired
    private ShopkeeperProfileRepository shopkeeperRepository;

    @Test
    @DisplayName("Should correctly calculate scrap settlement value and award loyalty points exactly once")
    void testSettlementCalculationAndLoyaltyAward() {
        CustomerProfile customer = customerRepository.findAll().get(0);
        ShopkeeperProfile shopkeeper = shopkeeperRepository.findByApprovalStatus(
                com.scrapconnection.enums.ApprovalStatus.APPROVED).get(0);

        int initialPoints = loyaltyService.getAvailablePoints(customer);

        ScrapBookingDto bookingDto = new ScrapBookingDto();
        bookingDto.setSelectedCategory("IRON");
        bookingDto.setEstimatedWeight(new BigDecimal("10.0"));
        bookingDto.setPickupAddress("Factory Road");
        bookingDto.setLocality("Madhapur");
        bookingDto.setPostalCode("500081");
        bookingDto.setPreferredDate(LocalDate.now());
        bookingDto.setTimeSlot("10:00 AM - 01:00 PM");
        bookingDto.setContactNumber("9876543210");

        ScrapBooking booking = bookingService.createBooking(customer, bookingDto);
        bookingService.acceptBooking(booking.getId(), shopkeeper);
        bookingService.startPickup(booking.getId(), shopkeeper);

        // Prepare Settlement: 10 kg of Iron at ₹30/kg = ₹300, minus ₹20 deductions = ₹280 Net
        SettlementDto settleDto = new SettlementDto();
        settleDto.setBookingId(booking.getId());
        settleDto.setSettlementMethod("CASH");
        settleDto.setDeductions(new BigDecimal("20.00"));
        settleDto.setDeductionReason("Rust dust loss");

        SettlementItemDto item = new SettlementItemDto(
                booking.getMaterials().get(0).getId(),
                booking.getMaterials().get(0).getCategory().getId(),
                "IRON",
                new BigDecimal("10.00"),
                new BigDecimal("30.00")
        );
        settleDto.setItems(List.of(item));

        Settlement settlement = settlementService.processSettlement(booking.getId(), shopkeeper, settleDto);

        assertNotNull(settlement.getId());
        assertEquals(new BigDecimal("300.00"), settlement.getGrossAmount());
        assertEquals(new BigDecimal("20.00"), settlement.getDeductions());
        assertEquals(new BigDecimal("280.00"), settlement.getNetAmount());
        assertEquals(SettlementMethod.CASH, settlement.getSettlementMethod());

        // Verify booking marked completed
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());

        // Verify loyalty points awarded: 1 point per ₹10 -> 280 / 10 = 28 points
        int updatedPoints = loyaltyService.getAvailablePoints(customer);
        assertEquals(initialPoints + 28, updatedPoints);

        // Attempt duplicate points award on the same completed booking
        loyaltyService.awardPointsForBooking(booking, new BigDecimal("280.00"));
        assertEquals(initialPoints + 28, loyaltyService.getAvailablePoints(customer),
                "Duplicate points must NOT be awarded for the same booking");
    }

    @Test
    @DisplayName("Should prevent duplicate settlement on the same scrap booking")
    void testDuplicateSettlementPrevention() {
        CustomerProfile customer = customerRepository.findAll().get(0);
        ShopkeeperProfile shopkeeper = shopkeeperRepository.findByApprovalStatus(
                com.scrapconnection.enums.ApprovalStatus.APPROVED).get(0);

        ScrapBookingDto bookingDto = new ScrapBookingDto();
        bookingDto.setSelectedCategory("PAPER");
        bookingDto.setEstimatedWeight(new BigDecimal("5.0"));
        bookingDto.setPickupAddress("Cyber Gateway");
        bookingDto.setLocality("Madhapur");
        bookingDto.setPostalCode("500081");
        bookingDto.setPreferredDate(LocalDate.now());
        bookingDto.setTimeSlot("10:00 AM - 01:00 PM");
        bookingDto.setContactNumber("9876543210");

        ScrapBooking booking = bookingService.createBooking(customer, bookingDto);
        bookingService.acceptBooking(booking.getId(), shopkeeper);

        SettlementDto settleDto = new SettlementDto();
        settleDto.setBookingId(booking.getId());
        settleDto.setSettlementMethod("CASH");
        settleDto.setDeductions(BigDecimal.ZERO);

        SettlementItemDto item = new SettlementItemDto(
                booking.getMaterials().get(0).getId(),
                booking.getMaterials().get(0).getCategory().getId(),
                "PAPER",
                new BigDecimal("5.00"),
                new BigDecimal("14.00")
        );
        settleDto.setItems(List.of(item));

        // First settlement succeeds
        settlementService.processSettlement(booking.getId(), shopkeeper, settleDto);

        // Second settlement attempt MUST throw ConflictException
        assertThrows(ConflictException.class, () -> {
            settlementService.processSettlement(booking.getId(), shopkeeper, settleDto);
        });
    }

    @Test
    @DisplayName("Should issue wallet coupon when COUPON settlement method is selected")
    void testCouponSettlementIssuance() {
        CustomerProfile customer = customerRepository.findAll().get(0);
        ShopkeeperProfile shopkeeper = shopkeeperRepository.findByApprovalStatus(
                com.scrapconnection.enums.ApprovalStatus.APPROVED).get(0);

        BigDecimal initialCouponBalance = couponService.getAvailableBalance(customer);

        ScrapBookingDto bookingDto = new ScrapBookingDto();
        bookingDto.setSelectedCategory("PAPER");
        bookingDto.setEstimatedWeight(new BigDecimal("10.0"));
        bookingDto.setPickupAddress("Silicon Valley");
        bookingDto.setLocality("Madhapur");
        bookingDto.setPostalCode("500081");
        bookingDto.setPreferredDate(LocalDate.now());
        bookingDto.setTimeSlot("10:00 AM - 01:00 PM");
        bookingDto.setContactNumber("9876543210");

        ScrapBooking booking = bookingService.createBooking(customer, bookingDto);
        bookingService.acceptBooking(booking.getId(), shopkeeper);

        SettlementDto settleDto = new SettlementDto();
        settleDto.setBookingId(booking.getId());
        settleDto.setSettlementMethod("COUPON"); // Coupon settlement!
        settleDto.setDeductions(BigDecimal.ZERO);

        SettlementItemDto item = new SettlementItemDto(
                booking.getMaterials().get(0).getId(),
                booking.getMaterials().get(0).getCategory().getId(),
                "PAPER",
                new BigDecimal("10.00"),
                new BigDecimal("15.00") // 10 * 15 = ₹150
        );
        settleDto.setItems(List.of(item));

        settlementService.processSettlement(booking.getId(), shopkeeper, settleDto);

        BigDecimal newBalance = couponService.getAvailableBalance(customer);
        assertEquals(initialCouponBalance.add(new BigDecimal("150.00")), newBalance);
    }
}
