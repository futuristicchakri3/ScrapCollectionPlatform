package com.scrapconnection.controller;

import com.scrapconnection.dto.MaterialPriceDto;
import com.scrapconnection.dto.ProductDto;
import com.scrapconnection.entity.*;
import com.scrapconnection.exception.UnauthorizedException;
import com.scrapconnection.repository.AdminAuditLogRepository;
import com.scrapconnection.security.SecurityUtils;
import com.scrapconnection.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final AdminAuditLogRepository auditLogRepository;

    public AdminController(AdminService adminService, AdminAuditLogRepository auditLogRepository) {
        this.adminService = adminService;
        this.auditLogRepository = auditLogRepository;
    }

    private User getAuthenticatedAdmin() {
        var userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null || !"ROLE_ADMIN".equals(userDetails.getRoleName())) {
            throw new UnauthorizedException("Administrator authorization required.");
        }
        return userDetails.getUser();
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        getAuthenticatedAdmin();
        model.addAttribute("stats", adminService.getDashboardStatistics());
        model.addAttribute("recentLogs", auditLogRepository.findTop20ByOrderByTimestampDesc());
        return "admin/dashboard";
    }

    @GetMapping("/shopkeepers")
    public String shopkeepersPage(Model model) {
        getAuthenticatedAdmin();
        model.addAttribute("shopkeepers", adminService.getAllShopkeepers());
        model.addAttribute("pendingShopkeepers", adminService.getPendingShopkeepers());
        return "admin/shopkeepers";
    }

    @PostMapping("/shopkeepers/{id}/approve")
    public String approveShopkeeper(@PathVariable("id") Long id,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        User admin = getAuthenticatedAdmin();
        try {
            adminService.approveShopkeeper(id, admin, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Shopkeeper approved successfully! Their account is now ACTIVE.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/shopkeepers";
    }

    @PostMapping("/shopkeepers/{id}/reject")
    public String rejectShopkeeper(@PathVariable("id") Long id,
                                  @RequestParam(value = "reason", required = false) String reason,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        User admin = getAuthenticatedAdmin();
        try {
            adminService.rejectShopkeeper(id, reason, admin, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Shopkeeper registration rejected.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/shopkeepers";
    }

    @GetMapping("/materials")
    public String materialsPage(Model model) {
        getAuthenticatedAdmin();
        model.addAttribute("materialPrices", adminService.getAllMaterialPrices());
        model.addAttribute("priceDto", new MaterialPriceDto());
        return "admin/materials";
    }

    @PostMapping("/materials/price")
    public String updatePrice(@Valid @ModelAttribute("priceDto") MaterialPriceDto dto,
                             BindingResult bindingResult,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        User admin = getAuthenticatedAdmin();
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid price parameters.");
            return "redirect:/admin/materials";
        }

        try {
            adminService.updateMaterialPrice(dto, admin, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Scrap rate per kg updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/materials";
    }

    @GetMapping("/products")
    public String productsPage(Model model) {
        getAuthenticatedAdmin();
        model.addAttribute("products", adminService.getAllProducts());
        model.addAttribute("productDto", new ProductDto());
        return "admin/products";
    }

    @PostMapping("/products/save")
    public String saveProduct(@Valid @ModelAttribute("productDto") ProductDto dto,
                             BindingResult bindingResult,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        User admin = getAuthenticatedAdmin();
        if (bindingResult.hasErrors()) {
            model.addAttribute("products", adminService.getAllProducts());
            return "admin/products";
        }

        try {
            adminService.saveOrUpdateProduct(dto, admin, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Marketplace product saved successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/bookings")
    public String bookingsPage(Model model) {
        getAuthenticatedAdmin();
        model.addAttribute("bookings", adminService.getAllBookings());
        return "admin/bookings";
    }

    @GetMapping("/orders")
    public String ordersPage(Model model) {
        getAuthenticatedAdmin();
        model.addAttribute("orders", adminService.getAllOrders());
        return "admin/orders";
    }

    @GetMapping("/audit-logs")
    public String auditLogsPage(Model model) {
        getAuthenticatedAdmin();
        model.addAttribute("auditLogs", auditLogRepository.findAllByOrderByTimestampDesc());
        return "admin/audit-logs";
    }
}
