package com.scrapconnection.controller;

import com.scrapconnection.repository.MaterialCategoryRepository;
import com.scrapconnection.repository.MaterialPriceRepository;
import com.scrapconnection.repository.ProductRepository;
import com.scrapconnection.repository.ScrapBookingRepository;
import com.scrapconnection.repository.UserRepository;
import com.scrapconnection.enums.Role;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final MaterialCategoryRepository categoryRepository;
    private final MaterialPriceRepository priceRepository;
    private final ProductRepository productRepository;
    private final ScrapBookingRepository bookingRepository;
    private final UserRepository userRepository;

    public HomeController(MaterialCategoryRepository categoryRepository,
                          MaterialPriceRepository priceRepository,
                          ProductRepository productRepository,
                          ScrapBookingRepository bookingRepository,
                          UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.priceRepository = priceRepository;
        this.productRepository = productRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("categories", categoryRepository.findByActiveTrue());
        model.addAttribute("prices", priceRepository.findByActiveTrue());
        model.addAttribute("featuredProducts", productRepository.findByActiveTrue().stream().limit(4).toList());
        model.addAttribute("totalCustomers", userRepository.countByRole(Role.ROLE_CUSTOMER));
        model.addAttribute("totalShopkeepers", userRepository.countByRole(Role.ROLE_SHOPKEEPER));
        model.addAttribute("totalBookings", bookingRepository.count());
        return "index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("categories", categoryRepository.findByActiveTrue());
        return "about";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("errorCode", 403);
        model.addAttribute("errorTitle", "Access Denied");
        model.addAttribute("errorMessage", "You do not have the required permissions to view this protected resource.");
        return "error/403";
    }
}
