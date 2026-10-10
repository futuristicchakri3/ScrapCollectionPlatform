package com.scrapconnection.service;

import com.scrapconnection.dto.BookingMaterialItemDto;
import com.scrapconnection.dto.ScrapBookingDto;
import com.scrapconnection.entity.*;
import com.scrapconnection.enums.ApprovalStatus;
import com.scrapconnection.enums.BookingStatus;
import com.scrapconnection.exception.BadRequestException;
import com.scrapconnection.exception.ConflictException;
import com.scrapconnection.exception.ResourceNotFoundException;
import com.scrapconnection.exception.UnauthorizedException;
import com.scrapconnection.repository.BookingMaterialRepository;
import com.scrapconnection.repository.BookingStatusHistoryRepository;
import com.scrapconnection.repository.MaterialCategoryRepository;
import com.scrapconnection.repository.ScrapBookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class BookingService {

    private final ScrapBookingRepository bookingRepository;
    private final BookingMaterialRepository bookingMaterialRepository;
    private final BookingStatusHistoryRepository statusHistoryRepository;
    private final MaterialCategoryRepository categoryRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public BookingService(ScrapBookingRepository bookingRepository,
                          BookingMaterialRepository bookingMaterialRepository,
                          BookingStatusHistoryRepository statusHistoryRepository,
                          MaterialCategoryRepository categoryRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingMaterialRepository = bookingMaterialRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public ScrapBooking createBooking(CustomerProfile customer, ScrapBookingDto dto) {
        if (dto.getPreferredDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Preferred pickup date cannot be in the past.");
        }

        String reference = generateBookingReference();

        ScrapBooking booking = new ScrapBooking(
                reference,
                customer,
                dto.getPickupAddress(),
                dto.getLocality(),
                dto.getPostalCode(),
                dto.getPreferredDate(),
                dto.getTimeSlot(),
                dto.getContactNumber(),
                dto.getAdditionalInstructions(),
                dto.getScrapImageUrl()
        );

        BigDecimal totalEstimatedWeight = BigDecimal.ZERO;

        // Process materials: either from list or from single category fallback
        if (dto.getMaterials() != null && !dto.getMaterials().isEmpty()) {
            for (BookingMaterialItemDto itemDto : dto.getMaterials()) {
                if (itemDto.getCategoryCode() != null && !itemDto.getCategoryCode().isBlank()) {
                    MaterialCategory category = categoryRepository.findByCode(itemDto.getCategoryCode())
                            .orElseThrow(() -> new BadRequestException("Invalid material category: " + itemDto.getCategoryCode()));

                    BigDecimal weight = itemDto.getEstimatedWeightKg() != null ? itemDto.getEstimatedWeightKg() : BigDecimal.ZERO;
                    BookingMaterial bm = new BookingMaterial(booking, category, weight);
                    booking.addMaterial(bm);
                    totalEstimatedWeight = totalEstimatedWeight.add(weight);
                }
            }
        } else if (dto.getSelectedCategory() != null && !dto.getSelectedCategory().isBlank()) {
            MaterialCategory category = categoryRepository.findByCode(dto.getSelectedCategory())
                    .orElseThrow(() -> new BadRequestException("Invalid material category: " + dto.getSelectedCategory()));

            BigDecimal weight = dto.getEstimatedWeight() != null ? dto.getEstimatedWeight() : BigDecimal.ONE;
            BookingMaterial bm = new BookingMaterial(booking, category, weight);
            booking.addMaterial(bm);
            totalEstimatedWeight = totalEstimatedWeight.add(weight);
        } else {
            // Default to PAPER if nothing specified
            MaterialCategory category = categoryRepository.findByCode("PAPER")
                    .orElseGet(() -> categoryRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new BadRequestException("No active material category found")));
            BigDecimal weight = dto.getEstimatedWeight() != null ? dto.getEstimatedWeight() : BigDecimal.valueOf(5);
            BookingMaterial bm = new BookingMaterial(booking, category, weight);
            booking.addMaterial(bm);
            totalEstimatedWeight = totalEstimatedWeight.add(weight);
        }

        booking.setTotalEstimatedWeight(totalEstimatedWeight);
        booking.setStatus(BookingStatus.REQUESTED);

        ScrapBooking saved = bookingRepository.save(booking);

        BookingStatusHistory history = new BookingStatusHistory(
                saved, null, BookingStatus.REQUESTED, customer.getUser(), "Booking requested by customer"
        );
        statusHistoryRepository.save(history);

        return saved;
    }

    @Transactional
    public ScrapBooking acceptBooking(Long bookingId, ShopkeeperProfile shopkeeper) {
        if (shopkeeper.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new UnauthorizedException("Only approved shopkeepers can accept pickup bookings.");
        }

        ScrapBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        // Conflict check to prevent double acceptance
        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new ConflictException("Booking " + booking.getBookingReference() +
                    " has already been accepted or is no longer available (current status: " + booking.getStatus() + ").");
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.ACCEPTED);
        booking.setAssignedShopkeeper(shopkeeper);

        ScrapBooking saved = bookingRepository.save(booking);

        BookingStatusHistory history = new BookingStatusHistory(
                saved, oldStatus, BookingStatus.ACCEPTED, shopkeeper.getUser(),
                "Booking accepted by shopkeeper " + shopkeeper.getShopName()
        );
        statusHistoryRepository.save(history);

        return saved;
    }

    @Transactional
    public ScrapBooking startPickup(Long bookingId, ShopkeeperProfile shopkeeper) {
        ScrapBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!shopkeeper.getId().equals(booking.getAssignedShopkeeper().getId())) {
            throw new UnauthorizedException("You are not assigned to this booking.");
        }

        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new BadRequestException("Cannot start pickup for booking with status: " + booking.getStatus());
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.PICKUP_IN_PROGRESS);

        ScrapBooking saved = bookingRepository.save(booking);

        BookingStatusHistory history = new BookingStatusHistory(
                saved, oldStatus, BookingStatus.PICKUP_IN_PROGRESS, shopkeeper.getUser(), "Pickup is now in progress"
        );
        statusHistoryRepository.save(history);

        return saved;
    }

    @Transactional
    public ScrapBooking cancelBooking(Long bookingId, CustomerProfile customer, String reason) {
        ScrapBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getCustomerProfile().getId().equals(customer.getId())) {
            throw new UnauthorizedException("You can only cancel your own bookings.");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Completed bookings cannot be cancelled.");
        }

        if (booking.getStatus() == BookingStatus.PICKUP_IN_PROGRESS) {
            throw new BadRequestException("Booking cannot be cancelled while pickup is in progress. Please contact the shopkeeper.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return booking;
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);

        ScrapBooking saved = bookingRepository.save(booking);

        BookingStatusHistory history = new BookingStatusHistory(
                saved, oldStatus, BookingStatus.CANCELLED, customer.getUser(),
                "Cancelled by customer: " + (reason != null && !reason.isBlank() ? reason : "No reason specified")
        );
        statusHistoryRepository.save(history);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<ScrapBooking> getAvailableBookingsForShopkeeper(ShopkeeperProfile shopkeeper) {
        // Shopkeeper sees REQUESTED bookings in their locality, or all REQUESTED if none found
        List<ScrapBooking> inLocality = bookingRepository.findByStatusAndLocalityIgnoreCaseOrderByCreatedAtDesc(
                BookingStatus.REQUESTED, shopkeeper.getLocality()
        );
        if (!inLocality.isEmpty()) {
            return inLocality;
        }
        return bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.REQUESTED);
    }

    @Transactional(readOnly = true)
    public List<ScrapBooking> getCustomerBookings(CustomerProfile customer) {
        return bookingRepository.findByCustomerProfileOrderByCreatedAtDesc(customer);
    }

    @Transactional(readOnly = true)
    public List<ScrapBooking> getShopkeeperBookings(ShopkeeperProfile shopkeeper) {
        return bookingRepository.findByAssignedShopkeeperOrderByCreatedAtDesc(shopkeeper);
    }

    @Transactional(readOnly = true)
    public List<ScrapBooking> getShopkeeperTodayBookings(ShopkeeperProfile shopkeeper) {
        return bookingRepository.findByAssignedShopkeeperAndPreferredDateOrderByCreatedAtDesc(shopkeeper, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public ScrapBooking getBookingByIdAndCustomer(Long bookingId, CustomerProfile customer) {
        ScrapBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!booking.getCustomerProfile().getId().equals(customer.getId())) {
            throw new UnauthorizedException("You cannot view this booking.");
        }
        return booking;
    }

    @Transactional(readOnly = true)
    public ScrapBooking getBookingByIdAndShopkeeper(Long bookingId, ShopkeeperProfile shopkeeper) {
        ScrapBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (booking.getAssignedShopkeeper() == null || !booking.getAssignedShopkeeper().getId().equals(shopkeeper.getId())) {
            // Also permitted if booking is in REQUESTED state so shopkeeper can view details before accepting
            if (booking.getStatus() != BookingStatus.REQUESTED) {
                throw new UnauthorizedException("You do not have access to this booking.");
            }
        }
        return booking;
    }

    private String generateBookingReference() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("SC-").append(datePart).append("-");
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        String ref = sb.toString();
        if (bookingRepository.findByBookingReference(ref).isPresent()) {
            return generateBookingReference();
        }
        return ref;
    }
}
