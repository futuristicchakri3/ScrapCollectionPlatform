package com.scrapconnection.config;

import com.scrapconnection.entity.*;
import com.scrapconnection.enums.AccountStatus;
import com.scrapconnection.enums.ApprovalStatus;
import com.scrapconnection.enums.Role;
import com.scrapconnection.repository.*;
import com.scrapconnection.service.LoyaltyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ShopkeeperProfileRepository shopkeeperProfileRepository;
    private final AddressRepository addressRepository;
    private final MaterialCategoryRepository categoryRepository;
    private final MaterialPriceRepository priceRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoyaltyService loyaltyService;

    public DataInitializer(UserRepository userRepository,
                           CustomerProfileRepository customerProfileRepository,
                           ShopkeeperProfileRepository shopkeeperProfileRepository,
                           AddressRepository addressRepository,
                           MaterialCategoryRepository categoryRepository,
                           MaterialPriceRepository priceRepository,
                           ProductRepository productRepository,
                           PasswordEncoder passwordEncoder,
                           LoyaltyService loyaltyService) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.shopkeeperProfileRepository = shopkeeperProfileRepository;
        this.addressRepository = addressRepository;
        this.categoryRepository = categoryRepository;
        this.priceRepository = priceRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
        this.loyaltyService = loyaltyService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initCategoriesAndPrices();
        initAdminUser();
        initSampleCustomer();
        initSampleShopkeepers();
        initSampleProducts();
        logger.info("=== Scrap Connection Data Initialization Completed ===");
    }

    private void initCategoriesAndPrices() {
        createCategoryIfAbsent("PAPER", "Paper & Books", "Newspapers, Books, Magazines, Office Paper", "newspaper", new BigDecimal("14.00"));
        createCategoryIfAbsent("CARDBOARD", "Cardboard & Cartons", "Corrugated Cardboard, Packaging Boxes", "box", new BigDecimal("10.00"));
        createCategoryIfAbsent("PLASTIC", "Plastic Materials", "PET Bottles, Hard Plastic, HDPE Containers", "bottle", new BigDecimal("16.00"));
        createCategoryIfAbsent("IRON", "Iron & Steel Scrap", "Iron Scrap, Steel Utensils, Rebar, Metal Rods", "hammer", new BigDecimal("28.00"));
        createCategoryIfAbsent("ALUMINIUM", "Aluminium Scrap", "Aluminium Cans, Utensils, Frames, Foil", "industry", new BigDecimal("110.00"));
        createCategoryIfAbsent("COPPER", "Copper Wires & Pipes", "Copper Wires, Pipes, Motors, Transformers", "zap", new BigDecimal("450.00"));
        createCategoryIfAbsent("GLASS", "Glass Bottles", "Glass Bottles, Broken Glass, Jars", "wine", new BigDecimal("2.50"));
        createCategoryIfAbsent("E_WASTE", "Electronic Waste (E-Waste)", "Electronic Scrap, PCBs, Old Phones, CPU Parts", "cpu", new BigDecimal("35.00"));
    }

    private void createCategoryIfAbsent(String code, String displayName, String desc, String icon, BigDecimal pricePerKg) {
        MaterialCategory category = categoryRepository.findByCode(code).orElseGet(() -> {
            MaterialCategory mc = new MaterialCategory(code, displayName, desc, icon, true);
            return categoryRepository.save(mc);
        });

        if (priceRepository.findByCategoryAndActiveTrue(category).isEmpty()) {
            MaterialPrice price = new MaterialPrice(category, pricePerKg, LocalDateTime.now(), true);
            priceRepository.save(price);
        }
    }

    private void initAdminUser() {
        if (userRepository.findByMobileNumber("9999999999").isEmpty()) {
            User admin = new User(
                    "9999999999",
                    passwordEncoder.encode("Admin@1234"),
                    Role.ROLE_ADMIN,
                    AccountStatus.ACTIVE
            );
            userRepository.save(admin);
            logger.info("Created Seed Administrator -> Mobile: 9999999999, Password: Admin@1234");
        }
    }

    private void initSampleCustomer() {
        if (userRepository.findByMobileNumber("9876543210").isEmpty()) {
            User user = new User(
                    "9876543210",
                    passwordEncoder.encode("Customer@123"),
                    Role.ROLE_CUSTOMER,
                    AccountStatus.ACTIVE
            );
            User saved = userRepository.save(user);

            CustomerProfile profile = new CustomerProfile(
                    saved,
                    "Rahul Sharma",
                    "rahul.sharma@example.com",
                    "Madhapur",
                    "Flat 302, Green Meadows, Madhapur, Hyderabad"
            );
            CustomerProfile savedProfile = customerProfileRepository.save(profile);

            Address addr = new Address(
                    savedProfile,
                    "Home",
                    "Flat 302, Green Meadows, Madhapur, Hyderabad",
                    "Madhapur",
                    "Hyderabad",
                    "500081",
                    "9876543210",
                    true
            );
            addressRepository.save(addr);

            loyaltyService.getOrCreateAccount(savedProfile);
            logger.info("Created Seed Customer -> Mobile: 9876543210, Password: Customer@123");
        }
    }

    private void initSampleShopkeepers() {
        // Approved Shopkeeper
        if (userRepository.findByMobileNumber("9123456789").isEmpty()) {
            User user = new User(
                    "9123456789",
                    passwordEncoder.encode("Shop@123"),
                    Role.ROLE_SHOPKEEPER,
                    AccountStatus.ACTIVE
            );
            User saved = userRepository.save(user);

            ShopkeeperProfile profile = new ShopkeeperProfile(
                    saved,
                    "Suresh Patel",
                    "Green Earth Scrap Traders",
                    "Plot 45, Industrial Estate, Madhapur",
                    "Madhapur",
                    "Hyderabad",
                    "500081",
                    "suresh.scrap@example.com",
                    "Authorized buyer of paper, cardboard, plastics, and all metals. Prompt doorstep weighing.",
                    "ALL"
            );
            profile.setApprovalStatus(ApprovalStatus.APPROVED);
            profile.setApprovedAt(LocalDateTime.now().minusDays(10));
            shopkeeperProfileRepository.save(profile);
            logger.info("Created Approved Seed Shopkeeper -> Mobile: 9123456789, Password: Shop@123");
        }

        // Pending Shopkeeper
        if (userRepository.findByMobileNumber("9811122233").isEmpty()) {
            User user = new User(
                    "9811122233",
                    passwordEncoder.encode("Shop@123"),
                    Role.ROLE_SHOPKEEPER,
                    AccountStatus.PENDING_APPROVAL
            );
            User saved = userRepository.save(user);

            ShopkeeperProfile profile = new ShopkeeperProfile(
                    saved,
                    "Venkatesh Rao",
                    "Eco Recycle Hub",
                    "Shop 12, Main Road, Gachibowli",
                    "Gachibowli",
                    "Hyderabad",
                    "500032",
                    "ecorecycle@example.com",
                    "Specialized in electronic scrap, cables, and battery recycling.",
                    "ALL"
            );
            profile.setApprovalStatus(ApprovalStatus.PENDING);
            shopkeeperProfileRepository.save(profile);
            logger.info("Created Pending Seed Shopkeeper -> Mobile: 9811122233, Password: Shop@123");
        }
    }

    private void initSampleProducts() {
        if (productRepository.count() == 0) {
            productRepository.save(new Product(
                    "Eco-Friendly Recycled Notebook (Pack of 3)",
                    "100% post-consumer recycled paper notebooks with chemical-free Kraft paper covers. Perfect for students and professionals.",
                    "Stationery",
                    new BigDecimal("180.00"),
                    45,
                    "notebook.jpg",
                    true
            ));

            productRepository.save(new Product(
                    "Upcycled Denim Shopping Tote Bag",
                    "Hand-stitched durable carry tote made from recycled denim jeans. Washable, heavy-duty, and stylish.",
                    "Fashion",
                    new BigDecimal("250.00"),
                    30,
                    "tote-bag.jpg",
                    true
            ));

            productRepository.save(new Product(
                    "Recycled Plastic Self-Watering Planter (Set of 2)",
                    "Molded entirely from ocean-bound and recycled household plastics. UV-stabilized with integrated drainage reservoirs.",
                    "Home & Garden",
                    new BigDecimal("320.00"),
                    25,
                    "planter.jpg",
                    true
            ));

            productRepository.save(new Product(
                    "Handcrafted Reclaimed Metal Desk Clock",
                    "Artisan desk clock crafted from reclaimed automobile gears and recycled steel sheets. Unique eco-industrial look.",
                    "Home Decor",
                    new BigDecimal("499.00"),
                    15,
                    "desk-clock.jpg",
                    true
            ));

            productRepository.save(new Product(
                    "Recycled Cotton Kitchen Towels (Pack of 4)",
                    "Ultra-absorbent waffle-weave kitchen dish towels made with 100% regenerated cotton yarn.",
                    "Home & Garden",
                    new BigDecimal("199.00"),
                    50,
                    "towels.jpg",
                    true
            ));

            productRepository.save(new Product(
                    "Biodegradable Wheat Straw Dinner Set",
                    "Eco tableware made from natural agricultural wheat straw fiber and recycled food-safe polymers. BPA-free & microwave safe.",
                    "Kitchenware",
                    new BigDecimal("299.00"),
                    60,
                    "dinner-set.jpg",
                    true
            ));
            logger.info("Created Seed Marketplace Products");
        }
    }
}
