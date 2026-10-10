package com.scrapconnection.service;

import com.scrapconnection.dto.SettlementDto;
import com.scrapconnection.dto.SettlementItemDto;
import com.scrapconnection.entity.*;
import com.scrapconnection.enums.BookingStatus;
import com.scrapconnection.enums.PaymentStatus;
import com.scrapconnection.enums.SettlementMethod;
import com.scrapconnection.exception.BadRequestException;
import com.scrapconnection.exception.ConflictException;
import com.scrapconnection.exception.ResourceNotFoundException;
import com.scrapconnection.exception.UnauthorizedException;
import com.scrapconnection.repository.BookingStatusHistoryRepository;
import com.scrapconnection.repository.MaterialCategoryRepository;
import com.scrapconnection.repository.MaterialPriceRepository;
import com.scrapconnection.repository.ScrapBookingRepository;
import com.scrapconnection.repository.SettlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final ScrapBookingRepository bookingRepository;
    private final MaterialPriceRepository priceRepository;
    private final MaterialCategoryRepository categoryRepository;
    private final BookingStatusHistoryRepository statusHistoryRepository;
    private final CouponService couponService;
    private final LoyaltyService loyaltyService;

    public SettlementService(SettlementRepository settlementRepository,
                             ScrapBookingRepository bookingRepository,
                             MaterialPriceRepository priceRepository,
                             MaterialCategoryRepository categoryRepository,
                             BookingStatusHistoryRepository statusHistoryRepository,
                             CouponService couponService,
                             LoyaltyService loyaltyService) {
        this.settlementRepository = settlementRepository;
        this.bookingRepository = bookingRepository;
        this.priceRepository = priceRepository;
        this.categoryRepository = categoryRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.couponService = couponService;
        this.loyaltyService = loyaltyService;
    }

    @Transactional
    public Settlement processSettlement(Long bookingId, ShopkeeperProfile shopkeeper, SettlementDto dto) {
        ScrapBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getAssignedShopkeeper() == null || !booking.getAssignedShopkeeper().getId().equals(shopkeeper.getId())) {
            throw new UnauthorizedException("You are not authorized to settle this booking.");
        }

        if (settlementRepository.existsByBooking(booking) || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ConflictException("Settlement has already been processed for this booking.");
        }

        if (booking.getStatus() != BookingStatus.PICKUP_IN_PROGRESS && booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new BadRequestException("Booking cannot be settled in its current status: " + booking.getStatus());
        }

        // 1. Calculate line items strictly on backend
        BigDecimal grossTotal = BigDecimal.ZERO;
        BigDecimal actualTotalWeight = BigDecimal.ZERO;

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (SettlementItemDto itemDto : dto.getItems()) {
                BigDecimal weight = itemDto.getActualWeightKg() != null ? itemDto.getActualWeightKg() : BigDecimal.ZERO;
                if (weight.compareTo(BigDecimal.ZERO) < 0) {
                    throw new BadRequestException("Weight cannot be negative.");
                }

                // Retrieve active standard rate for category, or fallback to submitted rate if authorized
                BigDecimal rate = itemDto.getPricePerKg();
                if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
                    MaterialPrice price = priceRepository.findById(itemDto.getCategoryId())
                            .orElseGet(() -> priceRepository.findByActiveTrue().stream().findFirst()
                                    .orElseThrow(() -> new BadRequestException("Price not found for category")));
                    rate = price.getPricePerKg();
                }

                BigDecimal lineTotal = weight.multiply(rate).setScale(2, RoundingMode.HALF_UP);
                grossTotal = grossTotal.add(lineTotal);
                actualTotalWeight = actualTotalWeight.add(weight);

                // Update or find corresponding BookingMaterial
                if (itemDto.getMaterialId() != null) {
                    for (BookingMaterial bm : booking.getMaterials()) {
                        if (bm.getId().equals(itemDto.getMaterialId())) {
                            bm.setActualWeightKg(weight);
                            bm.setPricePerKg(rate);
                            bm.setLineTotal(lineTotal);
                            break;
                        }
                    }
                }
            }
        } else {
            // If items not provided individually, fallback to booking's existing materials or estimated weight
            BigDecimal fallbackRate = BigDecimal.valueOf(15.00); // Standard paper/general scrap rate
            BigDecimal weight = booking.getTotalEstimatedWeight() != null ? booking.getTotalEstimatedWeight() : BigDecimal.valueOf(5.0);
            grossTotal = weight.multiply(fallbackRate).setScale(2, RoundingMode.HALF_UP);
            actualTotalWeight = weight;
        }

        // 2. Deductions
        BigDecimal deductions = dto.getDeductions() != null ? dto.getDeductions() : BigDecimal.ZERO;
        if (deductions.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Deductions cannot be negative.");
        }
        if (deductions.compareTo(grossTotal) > 0) {
            throw new BadRequestException("Deductions cannot exceed gross scrap value.");
        }

        BigDecimal netAmount = grossTotal.subtract(deductions).setScale(2, RoundingMode.HALF_UP);

        // 3. Settlement Method
        SettlementMethod method;
        try {
            method = SettlementMethod.valueOf(dto.getSettlementMethod().toUpperCase());
        } catch (Exception e) {
            method = SettlementMethod.CASH;
        }

        String transactionRef = method.name() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 4. Create and persist Settlement
        Settlement settlement = new Settlement(
                booking,
                shopkeeper,
                method,
                grossTotal,
                deductions,
                dto.getDeductionReason(),
                netAmount,
                PaymentStatus.CONFIRMED,
                transactionRef
        );
        Settlement savedSettlement = settlementRepository.save(settlement);

        // 5. Update Booking to COMPLETED
        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setActualTotalWeight(actualTotalWeight);
        booking.setTotalAmount(netAmount);
        bookingRepository.save(booking);

        BookingStatusHistory history = new BookingStatusHistory(
                booking, oldStatus, BookingStatus.COMPLETED, shopkeeper.getUser(),
                "Pickup completed and settled via " + method + ". Net amount: ₹" + netAmount
        );
        statusHistoryRepository.save(history);

        // 6. If method is COUPON, issue wallet coupon to customer
        if (method == SettlementMethod.COUPON) {
            couponService.issueCouponForSettlement(
                    savedSettlement,
                    booking.getCustomerProfile(),
                    shopkeeper,
                    netAmount
            );
        }

        // 7. Award loyalty points to customer
        loyaltyService.awardPointsForBooking(booking, netAmount);

        return savedSettlement;
    }

    @Transactional(readOnly = true)
    public Settlement getSettlementForBooking(Long bookingId) {
        return settlementRepository.findByBookingId(bookingId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Settlement> getShopkeeperSettlements(ShopkeeperProfile shopkeeper) {
        return settlementRepository.findByShopkeeperOrderBySettledAtDesc(shopkeeper);
    }
}
