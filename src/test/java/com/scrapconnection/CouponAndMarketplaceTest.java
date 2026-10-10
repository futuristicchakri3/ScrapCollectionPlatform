package com.scrapconnection;

import com.scrapconnection.dto.CheckoutDto;
import com.scrapconnection.entity.CustomerOrder;
import com.scrapconnection.entity.CustomerProfile;
import com.scrapconnection.entity.Product;
import com.scrapconnection.entity.ShopkeeperProfile;
import com.scrapconnection.exception.BadRequestException;
import com.scrapconnection.repository.CustomerProfileRepository;
import com.scrapconnection.repository.ProductRepository;
import com.scrapconnection.repository.ShopkeeperProfileRepository;
import com.scrapconnection.service.CouponService;
import com.scrapconnection.service.MarketplaceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CouponAndMarketplaceTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private MarketplaceService marketplaceService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerProfileRepository customerRepository;

    @Autowired
    private ShopkeeperProfileRepository shopkeeperRepository;

    @Test
    @DisplayName("Partial Coupon Redemption - Example 1: Product = ₹200, Coupon = ₹100")
    void testPartialCouponRedemptionExample1() {
        CustomerProfile customer = customerRepository.findAll().get(0);
        ShopkeeperProfile shopkeeper = shopkeeperRepository.findAll().get(0);

        // Issue coupon with balance ₹100
        couponService.issueCouponForSettlement(null, customer, shopkeeper, new BigDecimal("100.00"));
        BigDecimal availableBefore = couponService.getAvailableBalance(customer);

        // Redeem against order of ₹200
        BigDecimal orderTotal = new BigDecimal("200.00");
        BigDecimal couponUsed = couponService.redeemCouponBalance(customer, orderTotal, 1001L);

        // Formula assertions:
        // Coupon used = Minimum(200, 100) = 100
        assertEquals(new BigDecimal("100.00"), couponUsed);

        // Remaining payable = 200 - 100 = 100
        BigDecimal remainingPayable = orderTotal.subtract(couponUsed);
        assertEquals(new BigDecimal("100.00"), remainingPayable);

        // New coupon balance = Old coupon balance - 100
        BigDecimal newBalance = couponService.getAvailableBalance(customer);
        assertEquals(0, newBalance.compareTo(availableBefore.subtract(new BigDecimal("100.00"))));
    }

    @Test
    @DisplayName("Partial Coupon Redemption - Example 2: Product = ₹80, Coupon = ₹100")
    void testPartialCouponRedemptionExample2() {
        CustomerProfile customer = customerRepository.findAll().get(0);
        ShopkeeperProfile shopkeeper = shopkeeperRepository.findAll().get(0);

        couponService.issueCouponForSettlement(null, customer, shopkeeper, new BigDecimal("100.00"));
        BigDecimal availableBefore = couponService.getAvailableBalance(customer);

        BigDecimal orderTotal = new BigDecimal("80.00");
        BigDecimal couponUsed = couponService.redeemCouponBalance(customer, orderTotal, 1002L);

        // Formula assertions:
        // Coupon used = Minimum(80, 100) = 80
        assertEquals(new BigDecimal("80.00"), couponUsed);

        // Remaining payable = 80 - 80 = 0
        BigDecimal remainingPayable = orderTotal.subtract(couponUsed);
        assertEquals(0, remainingPayable.compareTo(BigDecimal.ZERO));

        // Remaining coupon balance = 100 - 80 = 20
        BigDecimal newBalance = couponService.getAvailableBalance(customer);
        assertEquals(0, newBalance.compareTo(availableBefore.subtract(new BigDecimal("80.00"))));
    }

    @Test
    @DisplayName("Partial Coupon Redemption - Example 3: Product = ₹150, Coupon = ₹100")
    void testPartialCouponRedemptionExample3() {
        CustomerProfile customer = customerRepository.findAll().get(0);
        ShopkeeperProfile shopkeeper = shopkeeperRepository.findAll().get(0);

        couponService.issueCouponForSettlement(null, customer, shopkeeper, new BigDecimal("100.00"));
        BigDecimal availableBefore = couponService.getAvailableBalance(customer);

        BigDecimal orderTotal = new BigDecimal("150.00");
        BigDecimal couponUsed = couponService.redeemCouponBalance(customer, orderTotal, 1003L);

        // Formula assertions:
        // Coupon used = Minimum(150, 100) = 100
        assertEquals(new BigDecimal("100.00"), couponUsed);

        // Remaining payable = 150 - 100 = 50
        BigDecimal remainingPayable = orderTotal.subtract(couponUsed);
        assertEquals(new BigDecimal("50.00"), remainingPayable);

        // Remaining coupon balance = 100 - 100 = 0
        BigDecimal newBalance = couponService.getAvailableBalance(customer);
        assertEquals(0, newBalance.compareTo(availableBefore.subtract(new BigDecimal("100.00"))));
    }

    @Test
    @DisplayName("Should validate product stock and prevent overselling during checkout")
    void testProductStockValidation() {
        CustomerProfile customer = customerRepository.findAll().get(0);
        Product product = productRepository.findAll().get(0);

        // Product has availableStock
        int currentStock = product.getAvailableStock();

        // Attempting to add quantity greater than stock must fail
        assertThrows(BadRequestException.class, () -> {
            marketplaceService.addToCart(customer, product.getId(), currentStock + 10);
        });
    }

    @Test
    @DisplayName("Should successfully process checkout, decrement stock, and apply coupon discount")
    void testCompleteCheckoutFlow() {
        CustomerProfile customer = customerRepository.findAll().get(0);
        ShopkeeperProfile shopkeeper = shopkeeperRepository.findAll().get(0);
        Product product = productRepository.findAll().get(0);

        int initialStock = product.getAvailableStock();

        // Give customer a coupon of ₹50
        couponService.issueCouponForSettlement(null, customer, shopkeeper, new BigDecimal("50.00"));

        // Add 1 product to cart
        marketplaceService.addToCart(customer, product.getId(), 1);

        CheckoutDto checkoutDto = new CheckoutDto();
        checkoutDto.setDeliveryAddress("Flat 101, Green Meadows");
        checkoutDto.setContactNumber("9876543210");
        checkoutDto.setPaymentMethod("CASH");
        checkoutDto.setApplyCoupon(true);

        CustomerOrder order = marketplaceService.processCheckout(customer, checkoutDto);

        assertNotNull(order.getId());
        assertNotNull(order.getOrderNumber());
        assertEquals(product.getPrice(), order.getSubtotal());

        // Coupon applied
        assertTrue(order.getCouponDiscount().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(order.getSubtotal().subtract(order.getCouponDiscount()), order.getPayableAmount());

        // Stock safely decremented
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(initialStock - 1, updatedProduct.getAvailableStock());

        // Cart cleared
        assertTrue(marketplaceService.getCartItems(customer).isEmpty());
    }
}
