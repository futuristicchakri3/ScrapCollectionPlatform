package com.scrapconnection.service;

import com.scrapconnection.dto.CheckoutDto;
import com.scrapconnection.entity.*;
import com.scrapconnection.enums.OrderStatus;
import com.scrapconnection.enums.PaymentStatus;
import com.scrapconnection.enums.SettlementMethod;
import com.scrapconnection.exception.BadRequestException;
import com.scrapconnection.exception.ResourceNotFoundException;
import com.scrapconnection.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MarketplaceService {

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderPaymentRepository paymentRepository;
    private final CouponService couponService;
    private final SecureRandom secureRandom = new SecureRandom();

    public MarketplaceService(ProductRepository productRepository,
                              CartItemRepository cartItemRepository,
                              CustomerOrderRepository orderRepository,
                              OrderItemRepository orderItemRepository,
                              OrderPaymentRepository paymentRepository,
                              CouponService couponService) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.couponService = couponService;
    }

    @Transactional(readOnly = true)
    public List<Product> getActiveProducts(String category, String query) {
        if (query != null && !query.isBlank() && category != null && !category.isBlank()) {
            return productRepository.findByNameContainingIgnoreCaseAndCategoryIgnoreCaseAndActiveTrue(query, category);
        } else if (query != null && !query.isBlank()) {
            return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(query);
        } else if (category != null && !category.isBlank()) {
            return productRepository.findByActiveTrueAndCategoryIgnoreCase(category);
        }
        return productRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return productRepository.findDistinctCategories();
    }

    @Transactional
    public void addToCart(CustomerProfile customer, Long productId, int quantity) {
        Product product = getProductById(productId);
        if (!product.isActive()) {
            throw new BadRequestException("This product is currently unavailable.");
        }
        if (product.getAvailableStock() < quantity) {
            throw new BadRequestException("Only " + product.getAvailableStock() + " units available in stock.");
        }

        CartItem cartItem = cartItemRepository.findByCustomerProfileAndProduct(customer, product)
                .orElseGet(() -> new CartItem(customer, product, 0));

        int newQty = cartItem.getQuantity() + quantity;
        if (newQty > product.getAvailableStock()) {
            throw new BadRequestException("Cannot add more. Total in cart exceeds available stock (" + product.getAvailableStock() + ").");
        }

        cartItem.setQuantity(newQty);
        cartItemRepository.save(cartItem);
    }

    @Transactional
    public void updateCartItemQuantity(CustomerProfile customer, Long productId, int quantity) {
        Product product = getProductById(productId);
        CartItem cartItem = cartItemRepository.findByCustomerProfileAndProduct(customer, product)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

        if (quantity <= 0) {
            cartItemRepository.delete(cartItem);
        } else {
            if (quantity > product.getAvailableStock()) {
                throw new BadRequestException("Requested quantity exceeds available stock (" + product.getAvailableStock() + ").");
            }
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
        }
    }

    @Transactional
    public void removeFromCart(CustomerProfile customer, Long productId) {
        Product product = getProductById(productId);
        cartItemRepository.findByCustomerProfileAndProduct(customer, product)
                .ifPresent(cartItemRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(CustomerProfile customer) {
        return cartItemRepository.findByCustomerProfileOrderByAddedAtDesc(customer);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateCartSubtotal(CustomerProfile customer) {
        List<CartItem> items = getCartItems(customer);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            BigDecimal itemTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(itemTotal);
        }
        return subtotal;
    }

    @Transactional
    public CustomerOrder processCheckout(CustomerProfile customer, CheckoutDto dto) {
        List<CartItem> cartItems = cartItemRepository.findByCustomerProfileOrderByAddedAtDesc(customer);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Your cart is empty. Please add recycled products before checking out.");
        }

        // 1. Verify stock & recalculate server-side subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            if (!product.isActive()) {
                throw new BadRequestException("Product '" + product.getName() + "' is no longer active.");
            }
            if (product.getAvailableStock() < item.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product '" + product.getName() +
                        "'. Requested: " + item.getQuantity() + ", Available: " + product.getAvailableStock());
            }

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(itemTotal);
        }

        // 2. Handle Coupon Deduction
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (dto.isApplyCoupon()) {
            BigDecimal availableCouponBalance = couponService.getAvailableBalance(customer);
            // Coupon used = Minimum(subtotal, availableCouponBalance)
            couponDiscount = subtotal.min(availableCouponBalance);
        }

        BigDecimal payableAmount = subtotal.subtract(couponDiscount);

        SettlementMethod paymentMethod;
        try {
            paymentMethod = SettlementMethod.valueOf(dto.getPaymentMethod().toUpperCase());
        } catch (Exception e) {
            paymentMethod = payableAmount.compareTo(BigDecimal.ZERO) == 0 ? SettlementMethod.COUPON : SettlementMethod.CASH;
        }

        // 3. Create Order
        String orderNumber = generateOrderNumber();
        PaymentStatus paymentStatus = payableAmount.compareTo(BigDecimal.ZERO) == 0 ? PaymentStatus.CONFIRMED : PaymentStatus.PENDING;

        CustomerOrder order = new CustomerOrder(
                orderNumber,
                customer,
                subtotal,
                couponDiscount,
                payableAmount,
                paymentMethod,
                paymentStatus,
                dto.getDeliveryAddress(),
                dto.getContactNumber()
        );
        CustomerOrder savedOrder = orderRepository.save(order);

        // 4. Create OrderItems & Safely Decrement Product Stock
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            // Decrement stock
            product.setAvailableStock(product.getAvailableStock() - item.getQuantity());
            productRepository.save(product);

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            OrderItem orderItem = new OrderItem(
                    savedOrder,
                    product,
                    product.getName(),
                    product.getPrice(),
                    item.getQuantity(),
                    lineTotal
            );
            orderItemRepository.save(orderItem);
        }

        // 5. Deduct Coupon Balance atomically if applicable
        if (couponDiscount.compareTo(BigDecimal.ZERO) > 0) {
            couponService.redeemCouponBalance(customer, couponDiscount, savedOrder.getId());
        }

        // 6. Record Payment
        String payRef = "PAY-" + secureRandom.nextInt(900000);
        OrderPayment payment = new OrderPayment(
                savedOrder,
                paymentMethod,
                payableAmount,
                couponDiscount,
                paymentStatus,
                payRef
        );
        paymentRepository.save(payment);

        // 7. Clear Cart
        cartItemRepository.deleteByCustomerProfile(customer);

        return savedOrder;
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> getCustomerOrders(CustomerProfile customer) {
        return orderRepository.findByCustomerProfileOrderByCreatedAtDesc(customer);
    }

    @Transactional(readOnly = true)
    public CustomerOrder getCustomerOrder(String orderNumber, CustomerProfile customer) {
        CustomerOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));
        if (!order.getCustomerProfile().getId().equals(customer.getId())) {
            throw new BadRequestException("You do not have permission to view this order.");
        }
        return order;
    }

    private String generateOrderNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = 1000 + secureRandom.nextInt(9000);
        return "ORD-" + datePart + "-" + rand;
    }
}
