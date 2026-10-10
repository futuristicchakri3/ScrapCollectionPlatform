package com.scrapconnection.service;

import com.scrapconnection.dto.MaterialPriceDto;
import com.scrapconnection.dto.ProductDto;
import com.scrapconnection.entity.*;
import com.scrapconnection.enums.AccountStatus;
import com.scrapconnection.enums.ApprovalStatus;
import com.scrapconnection.enums.BookingStatus;
import com.scrapconnection.enums.Role;
import com.scrapconnection.exception.ResourceNotFoundException;
import com.scrapconnection.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ShopkeeperProfileRepository shopkeeperRepository;
    private final CustomerProfileRepository customerRepository;
    private final ScrapBookingRepository bookingRepository;
    private final SettlementRepository settlementRepository;
    private final CouponRepository couponRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final ProductRepository productRepository;
    private final CustomerOrderRepository orderRepository;
    private final MaterialCategoryRepository categoryRepository;
    private final MaterialPriceRepository priceRepository;
    private final AuditService auditService;

    public AdminService(UserRepository userRepository,
                        ShopkeeperProfileRepository shopkeeperRepository,
                        CustomerProfileRepository customerRepository,
                        ScrapBookingRepository bookingRepository,
                        SettlementRepository settlementRepository,
                        CouponRepository couponRepository,
                        LoyaltyAccountRepository loyaltyAccountRepository,
                        ProductRepository productRepository,
                        CustomerOrderRepository orderRepository,
                        MaterialCategoryRepository categoryRepository,
                        MaterialPriceRepository priceRepository,
                        AuditService auditService) {
        this.userRepository = userRepository;
        this.shopkeeperRepository = shopkeeperRepository;
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
        this.settlementRepository = settlementRepository;
        this.couponRepository = couponRepository;
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.categoryRepository = categoryRepository;
        this.priceRepository = priceRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalCustomers", userRepository.countByRole(Role.ROLE_CUSTOMER));
        stats.put("totalShopkeepers", userRepository.countByRole(Role.ROLE_SHOPKEEPER));
        stats.put("pendingApprovals", shopkeeperRepository.countByApprovalStatus(ApprovalStatus.PENDING));
        stats.put("approvedShopkeepers", shopkeeperRepository.countByApprovalStatus(ApprovalStatus.APPROVED));
        stats.put("rejectedShopkeepers", shopkeeperRepository.countByApprovalStatus(ApprovalStatus.REJECTED));

        stats.put("totalBookings", bookingRepository.count());
        stats.put("completedPickups", bookingRepository.countByStatus(BookingStatus.COMPLETED));
        stats.put("totalScrapValue", bookingRepository.sumTotalAmountPlatformCompleted());
        stats.put("totalSettledAmount", settlementRepository.sumTotalSettledAmountPlatform());

        stats.put("totalCouponsIssued", couponRepository.sumTotalIssuedAmountPlatform());
        stats.put("totalActiveCouponsBalance", couponRepository.sumTotalActiveBalancePlatform());
        stats.put("totalLoyaltyPoints", loyaltyAccountRepository.sumTotalPointsIssuedPlatform());

        stats.put("totalProducts", productRepository.count());
        stats.put("totalOrders", orderRepository.count());
        stats.put("totalRevenue", orderRepository.sumTotalPaidOrdersPlatform());

        return stats;
    }

    @Transactional
    public void approveShopkeeper(Long shopkeeperId, User adminUser, String ipAddress) {
        ShopkeeperProfile shopkeeper = shopkeeperRepository.findById(shopkeeperId)
                .orElseThrow(() -> new ResourceNotFoundException("Shopkeeper not found with id: " + shopkeeperId));

        shopkeeper.setApprovalStatus(ApprovalStatus.APPROVED);
        shopkeeper.setApprovedAt(LocalDateTime.now());
        shopkeeperRepository.save(shopkeeper);

        User user = shopkeeper.getUser();
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        auditService.logAction(
                adminUser, "APPROVE_SHOPKEEPER", "ShopkeeperProfile", shopkeeperId,
                "Approved shop: " + shopkeeper.getShopName() + " (" + shopkeeper.getOwnerName() + ")", ipAddress
        );
    }

    @Transactional
    public void rejectShopkeeper(Long shopkeeperId, String reason, User adminUser, String ipAddress) {
        ShopkeeperProfile shopkeeper = shopkeeperRepository.findById(shopkeeperId)
                .orElseThrow(() -> new ResourceNotFoundException("Shopkeeper not found with id: " + shopkeeperId));

        shopkeeper.setApprovalStatus(ApprovalStatus.REJECTED);
        shopkeeper.setRejectionReason(reason);
        shopkeeperRepository.save(shopkeeper);

        User user = shopkeeper.getUser();
        user.setStatus(AccountStatus.REJECTED);
        userRepository.save(user);

        auditService.logAction(
                adminUser, "REJECT_SHOPKEEPER", "ShopkeeperProfile", shopkeeperId,
                "Rejected shop: " + shopkeeper.getShopName() + ". Reason: " + reason, ipAddress
        );
    }

    @Transactional
    public void updateMaterialPrice(MaterialPriceDto dto, User adminUser, String ipAddress) {
        MaterialCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Material category not found"));

        MaterialPrice activePrice = priceRepository.findByCategoryAndActiveTrue(category)
                .orElseGet(() -> new MaterialPrice(category, dto.getPricePerKg(), LocalDateTime.now(), true));

        BigDecimal oldPrice = activePrice.getPricePerKg();
        activePrice.setPricePerKg(dto.getPricePerKg());
        activePrice.setEffectiveFrom(LocalDateTime.now());
        priceRepository.save(activePrice);

        auditService.logAction(
                adminUser, "UPDATE_MATERIAL_PRICE", "MaterialPrice", activePrice.getId(),
                "Updated " + category.getDisplayName() + " rate from ₹" + oldPrice + " to ₹" + dto.getPricePerKg(), ipAddress
        );
    }

    @Transactional
    public Product saveOrUpdateProduct(ProductDto dto, User adminUser, String ipAddress) {
        Product product;
        String action;
        if (dto.getId() != null) {
            product = productRepository.findById(dto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            action = "UPDATE_PRODUCT";
        } else {
            product = new Product();
            action = "CREATE_PRODUCT";
        }

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setCategory(dto.getCategory());
        product.setPrice(dto.getPrice());
        product.setAvailableStock(dto.getAvailableStock());
        product.setImageUrl(dto.getImageUrl());
        product.setActive(dto.isActive());

        Product saved = productRepository.save(product);

        auditService.logAction(
                adminUser, action, "Product", saved.getId(),
                action + ": " + saved.getName() + " (Price: ₹" + saved.getPrice() + ", Stock: " + saved.getAvailableStock() + ")", ipAddress
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<ShopkeeperProfile> getAllShopkeepers() {
        return shopkeeperRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ShopkeeperProfile> getPendingShopkeepers() {
        return shopkeeperRepository.findByApprovalStatus(ApprovalStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<MaterialPrice> getAllMaterialPrices() {
        return priceRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ScrapBooking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }
}
