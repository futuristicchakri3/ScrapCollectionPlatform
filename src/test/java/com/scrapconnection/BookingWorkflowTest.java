package com.scrapconnection;

import com.scrapconnection.dto.ScrapBookingDto;
import com.scrapconnection.entity.CustomerProfile;
import com.scrapconnection.entity.ScrapBooking;
import com.scrapconnection.entity.ShopkeeperProfile;
import com.scrapconnection.enums.BookingStatus;
import com.scrapconnection.exception.ConflictException;
import com.scrapconnection.exception.UnauthorizedException;
import com.scrapconnection.repository.CustomerProfileRepository;
import com.scrapconnection.repository.ShopkeeperProfileRepository;
import com.scrapconnection.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class BookingWorkflowTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CustomerProfileRepository customerRepository;

    @Autowired
    private ShopkeeperProfileRepository shopkeeperRepository;

    @Test
    @DisplayName("Should successfully create a scrap pickup booking in REQUESTED state")
    void testBookingCreation() {
        CustomerProfile customer = customerRepository.findAll().get(0);

        ScrapBookingDto dto = new ScrapBookingDto();
        dto.setSelectedCategory("PAPER");
        dto.setEstimatedWeight(new BigDecimal("15.0"));
        dto.setPickupAddress("Flat 101, Cyber Towers");
        dto.setLocality("Madhapur");
        dto.setPostalCode("500081");
        dto.setPreferredDate(LocalDate.now().plusDays(1));
        dto.setTimeSlot("10:00 AM - 01:00 PM");
        dto.setContactNumber("9876543210");

        ScrapBooking booking = bookingService.createBooking(customer, dto);

        assertNotNull(booking.getId());
        assertNotNull(booking.getBookingReference());
        assertEquals(BookingStatus.REQUESTED, booking.getStatus());
        assertEquals(new BigDecimal("15.0"), booking.getTotalEstimatedWeight());
        assertNull(booking.getAssignedShopkeeper());
    }

    @Test
    @DisplayName("Should prevent double acceptance of the same booking by two shopkeepers")
    void testDoubleAcceptanceConflict() {
        CustomerProfile customer = customerRepository.findAll().get(0);
        ShopkeeperProfile approvedShopkeeper = shopkeeperRepository.findByApprovalStatus(
                com.scrapconnection.enums.ApprovalStatus.APPROVED).get(0);

        ScrapBookingDto dto = new ScrapBookingDto();
        dto.setSelectedCategory("IRON");
        dto.setEstimatedWeight(new BigDecimal("20.0"));
        dto.setPickupAddress("Plot 5, Hitech City");
        dto.setLocality("Madhapur");
        dto.setPostalCode("500081");
        dto.setPreferredDate(LocalDate.now().plusDays(2));
        dto.setTimeSlot("12:00 PM - 03:00 PM");
        dto.setContactNumber("9876543210");

        ScrapBooking booking = bookingService.createBooking(customer, dto);

        // First shopkeeper accepts successfully
        ScrapBooking accepted = bookingService.acceptBooking(booking.getId(), approvedShopkeeper);
        assertEquals(BookingStatus.ACCEPTED, accepted.getStatus());
        assertEquals(approvedShopkeeper.getId(), accepted.getAssignedShopkeeper().getId());

        // Second acceptance attempt MUST throw ConflictException
        assertThrows(ConflictException.class, () -> {
            bookingService.acceptBooking(booking.getId(), approvedShopkeeper);
        });
    }

    @Test
    @DisplayName("Should reject unapproved shopkeeper from accepting bookings")
    void testUnapprovedShopkeeperCannotAccept() {
        CustomerProfile customer = customerRepository.findAll().get(0);
        ShopkeeperProfile pendingShopkeeper = shopkeeperRepository.findByApprovalStatus(
                com.scrapconnection.enums.ApprovalStatus.PENDING).get(0);

        ScrapBookingDto dto = new ScrapBookingDto();
        dto.setSelectedCategory("PLASTIC");
        dto.setEstimatedWeight(new BigDecimal("10.0"));
        dto.setPickupAddress("Plot 10, Gachibowli");
        dto.setLocality("Gachibowli");
        dto.setPostalCode("500032");
        dto.setPreferredDate(LocalDate.now().plusDays(1));
        dto.setTimeSlot("10:00 AM - 01:00 PM");
        dto.setContactNumber("9876543210");

        ScrapBooking booking = bookingService.createBooking(customer, dto);

        assertThrows(UnauthorizedException.class, () -> {
            bookingService.acceptBooking(booking.getId(), pendingShopkeeper);
        });
    }

    @Test
    @DisplayName("Should prevent customer from accessing another customer's booking")
    void testCrossCustomerAccessPrevented() {
        CustomerProfile customer = customerRepository.findAll().get(0);

        ScrapBookingDto dto = new ScrapBookingDto();
        dto.setSelectedCategory("PAPER");
        dto.setEstimatedWeight(new BigDecimal("5.0"));
        dto.setPickupAddress("Private address");
        dto.setLocality("Madhapur");
        dto.setPostalCode("500081");
        dto.setPreferredDate(LocalDate.now().plusDays(1));
        dto.setTimeSlot("10:00 AM - 01:00 PM");
        dto.setContactNumber("9876543210");

        ScrapBooking booking = bookingService.createBooking(customer, dto);

        // Dummy other customer
        CustomerProfile otherCustomer = new CustomerProfile();
        otherCustomer.setId(9999L);

        assertThrows(UnauthorizedException.class, () -> {
            bookingService.getBookingByIdAndCustomer(booking.getId(), otherCustomer);
        });
    }
}
