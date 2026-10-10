package com.scrapconnection.controller;

import com.scrapconnection.dto.CheckoutDto;
import com.scrapconnection.entity.Address;
import com.scrapconnection.entity.CartItem;
import com.scrapconnection.entity.CustomerOrder;
import com.scrapconnection.entity.CustomerProfile;
import com.scrapconnection.entity.Product;
import com.scrapconnection.exception.BadRequestException;
import com.scrapconnection.exception.UnauthorizedException;
import com.scrapconnection.repository.AddressRepository;
import com.scrapconnection.security.SecurityUtils;
import com.scrapconnection.service.CouponService;
import com.scrapconnection.service.MarketplaceService;
import com.scrapconnection.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/marketplace")
public class MarketplaceController {

    private final MarketplaceService marketplaceService;
    private final CouponService couponService;
    private final UserService userService;
    private final AddressRepository addressRepository;

    public MarketplaceController(MarketplaceService marketplaceService,
                                 CouponService couponService,
                                 UserService userService,
                                 AddressRepository addressRepository) {
        this.marketplaceService = marketplaceService;
        this.couponService = couponService;
        this.userService = userService;
        this.addressRepository = addressRepository;
    }

    private CustomerProfile getAuthenticatedCustomerOrNull() {
        var userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails != null && "ROLE_CUSTOMER".equals(userDetails.getRoleName())) {
            return userService.getCustomerProfile(userDetails.getUser());
        }
        return null;
    }

    @GetMapping
    public String catalog(@RequestParam(value = "category", required = false) String category,
                          @RequestParam(value = "search", required = false) String search,
                          Model model) {
        List<Product> products = marketplaceService.getActiveProducts(category, search);
        List<String> categories = marketplaceService.getCategories();

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("searchQuery", search);

        CustomerProfile customer = getAuthenticatedCustomerOrNull();
        if (customer != null) {
            model.addAttribute("cartCount", marketplaceService.getCartItems(customer).size());
            model.addAttribute("couponBalance", couponService.getAvailableBalance(customer));
        }

        return "marketplace/catalog";
    }

    @GetMapping("/product/{id}")
    public String productDetails(@PathVariable("id") Long id, Model model) {
        Product product = marketplaceService.getProductById(id);
        model.addAttribute("product", product);

        CustomerProfile customer = getAuthenticatedCustomerOrNull();
        if (customer != null) {
            model.addAttribute("cartCount", marketplaceService.getCartItems(customer).size());
        }

        return "marketplace/product-detail";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("productId") Long productId,
                            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                            RedirectAttributes redirectAttributes) {
        CustomerProfile customer = getAuthenticatedCustomerOrNull();
        if (customer == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please log in as a customer to add items to your cart.");
            return "redirect:/login";
        }

        try {
            marketplaceService.addToCart(customer, productId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Product added to cart!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/marketplace/cart";
    }

    @GetMapping("/cart")
    public String viewCart(Model model) {
        CustomerProfile customer = getAuthenticatedCustomerOrNull();
        if (customer == null) {
            return "redirect:/login";
        }

        List<CartItem> cartItems = marketplaceService.getCartItems(customer);
        BigDecimal subtotal = marketplaceService.calculateCartSubtotal(customer);
        BigDecimal availableCouponBalance = couponService.getAvailableBalance(customer);

        model.addAttribute("customer", customer);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("availableCouponBalance", availableCouponBalance);

        return "marketplace/cart";
    }

    @PostMapping("/cart/update")
    public String updateCart(@RequestParam("productId") Long productId,
                             @RequestParam("quantity") int quantity,
                             RedirectAttributes redirectAttributes) {
        CustomerProfile customer = getAuthenticatedCustomerOrNull();
        if (customer == null) {
            return "redirect:/login";
        }

        try {
            marketplaceService.updateCartItemQuantity(customer, productId, quantity);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/marketplace/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam("productId") Long productId,
                                 RedirectAttributes redirectAttributes) {
        CustomerProfile customer = getAuthenticatedCustomerOrNull();
        if (customer == null) {
            return "redirect:/login";
        }

        marketplaceService.removeFromCart(customer, productId);
        redirectAttributes.addFlashAttribute("successMessage", "Item removed from cart.");
        return "redirect:/marketplace/cart";
    }

    @GetMapping("/checkout")
    public String checkoutPage(Model model) {
        CustomerProfile customer = getAuthenticatedCustomerOrNull();
        if (customer == null) {
            return "redirect:/login";
        }

        List<CartItem> cartItems = marketplaceService.getCartItems(customer);
        if (cartItems.isEmpty()) {
            return "redirect:/marketplace";
        }

        BigDecimal subtotal = marketplaceService.calculateCartSubtotal(customer);
        BigDecimal availableCouponBalance = couponService.getAvailableBalance(customer);

        // Prepopulate checkout form
        CheckoutDto checkoutDto = new CheckoutDto();
        checkoutDto.setDeliveryAddress(customer.getDefaultAddress());
        checkoutDto.setContactNumber(customer.getUser().getMobileNumber());
        checkoutDto.setPaymentMethod("CASH");
        checkoutDto.setApplyCoupon(availableCouponBalance.compareTo(BigDecimal.ZERO) > 0);

        List<Address> savedAddresses = addressRepository.findByCustomerProfile(customer);

        model.addAttribute("customer", customer);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("availableCouponBalance", availableCouponBalance);
        model.addAttribute("checkoutDto", checkoutDto);
        model.addAttribute("savedAddresses", savedAddresses);

        return "marketplace/checkout";
    }

    @PostMapping("/checkout")
    public String processCheckout(@Valid @ModelAttribute("checkoutDto") CheckoutDto dto,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        CustomerProfile customer = getAuthenticatedCustomerOrNull();
        if (customer == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            List<CartItem> cartItems = marketplaceService.getCartItems(customer);
            BigDecimal subtotal = marketplaceService.calculateCartSubtotal(customer);
            BigDecimal availableCouponBalance = couponService.getAvailableBalance(customer);

            model.addAttribute("customer", customer);
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("availableCouponBalance", availableCouponBalance);
            model.addAttribute("savedAddresses", addressRepository.findByCustomerProfile(customer));
            return "marketplace/checkout";
        }

        try {
            CustomerOrder order = marketplaceService.processCheckout(customer, dto);
            return "redirect:/marketplace/order-confirmation/" + order.getOrderNumber();
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/marketplace/checkout";
        }
    }

    @GetMapping("/order-confirmation/{orderNumber}")
    public String orderConfirmation(@PathVariable("orderNumber") String orderNumber, Model model) {
        CustomerProfile customer = getAuthenticatedCustomerOrNull();
        if (customer == null) {
            return "redirect:/login";
        }

        CustomerOrder order = marketplaceService.getCustomerOrder(orderNumber, customer);
        model.addAttribute("order", order);
        return "marketplace/order-confirmation";
    }

    @GetMapping("/orders")
    public String myOrders(Model model) {
        CustomerProfile customer = getAuthenticatedCustomerOrNull();
        if (customer == null) {
            return "redirect:/login";
        }

        List<CustomerOrder> orders = marketplaceService.getCustomerOrders(customer);
        model.addAttribute("customer", customer);
        model.addAttribute("orders", orders);
        return "marketplace/orders";
    }
}
