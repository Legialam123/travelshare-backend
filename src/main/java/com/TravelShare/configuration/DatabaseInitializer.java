package com.TravelShare.configuration;

import com.TravelShare.dto.request.CurrencyCreationRequest;
import com.TravelShare.entity.Category;
import com.TravelShare.entity.User;
import com.TravelShare.repository.CurrencyRepository;
import com.TravelShare.repository.CategoryRepository;
import com.TravelShare.repository.UserRepository;
import com.TravelShare.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrencyRepository currencyRepository;
    private final CategoryRepository categoryRepository;
    private final CurrencyService currencyService;


    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        initAdminAccount();
        initCurrencies();
        initCategories();
        initRegularUsers();
    }
    private void initRegularUsers() {
        createUserIfNotExists("lam123", "123456", "legialam95@gmail.com", "Lam Le");
        createUserIfNotExists("liem123", "123456", "liem@example.com", "Liem Le");
    }

    private void createUserIfNotExists(String username, String rawPassword, String email, String fullName) {
        if (userRepository.findByUsername(username).isEmpty()) {
            log.info("Creating user account: {}", username);
            User user = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(rawPassword))
                    .email(email)
                    .fullName(fullName)
                    .role("USER")
                    .createdAt(LocalDateTime.now())
                    .groups(new HashSet<>())
                    .profileImages(new HashSet<>())
                    .active(true)
                    .build();
            userRepository.save(user);
            log.info("User account {} created successfully", username);
        } else {
            log.info("User account {} already exists", username);
        }
    }
    private void initAdminAccount() {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            log.info("Creating admin account");
            User admin = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .email(adminEmail)
                    .fullName("System Administrator")
                    .role("ADMIN")
                    .createdAt(LocalDateTime.now())
                    .groups(new HashSet<>())
                    .profileImages(new HashSet<>())
                    .active(true)
                    .build();
            userRepository.save(admin);
            log.info("Admin account created successfully");
        } else {
            log.info("Admin account already exists");
        }
    }

    private void initCurrencies() {
        Map<String, String[]> currencies = new HashMap<>();

        // Major currencies
        currencies.put("USD", new String[]{"US Dollar", "$"});
        currencies.put("EUR", new String[]{"Euro", "€"});
        currencies.put("GBP", new String[]{"British Pound", "£"});
        currencies.put("JPY", new String[]{"Japanese Yen", "¥"});
        currencies.put("VND", new String[]{"Vietnamese Dong", "₫"});
        currencies.put("CNY", new String[]{"Chinese Yuan", "¥"});
        currencies.put("AUD", new String[]{"Australian Dollar", "A$"});
        currencies.put("CAD", new String[]{"Canadian Dollar", "C$"});

        // Southeast Asia
        currencies.put("THB", new String[]{"Thai Baht", "฿"});
        currencies.put("SGD", new String[]{"Singapore Dollar", "S$"});
        currencies.put("MYR", new String[]{"Malaysian Ringgit", "RM"});
        currencies.put("IDR", new String[]{"Indonesian Rupiah", "Rp"});
        currencies.put("PHP", new String[]{"Philippine Peso", "₱"});

        // East Asia
        currencies.put("KRW", new String[]{"South Korean Won", "₩"});
        currencies.put("HKD", new String[]{"Hong Kong Dollar", "HK$"});
        currencies.put("TWD", new String[]{"Taiwan Dollar", "NT$"});

        // South Asia
        currencies.put("INR", new String[]{"Indian Rupee", "₹"});
        currencies.put("PKR", new String[]{"Pakistani Rupee", "₨"});
        currencies.put("LKR", new String[]{"Sri Lankan Rupee", "Rs"});
        currencies.put("BDT", new String[]{"Bangladeshi Taka", "৳"});

        // Middle East
        currencies.put("AED", new String[]{"UAE Dirham", "د.إ"});
        currencies.put("SAR", new String[]{"Saudi Riyal", "﷼"});
        currencies.put("QAR", new String[]{"Qatari Riyal", "ر.ق"});
        currencies.put("KWD", new String[]{"Kuwaiti Dinar", "د.ك"});

        // Europe
        currencies.put("CHF", new String[]{"Swiss Franc", "CHF"});
        currencies.put("NOK", new String[]{"Norwegian Krone", "kr"});
        currencies.put("SEK", new String[]{"Swedish Krona", "kr"});
        currencies.put("DKK", new String[]{"Danish Krone", "kr"});
        currencies.put("PLN", new String[]{"Polish Zloty", "zł"});
        currencies.put("CZK", new String[]{"Czech Koruna", "Kč"});
        currencies.put("HUF", new String[]{"Hungarian Forint", "Ft"});
        currencies.put("RUB", new String[]{"Russian Ruble", "₽"});

        // Americas
        currencies.put("BRL", new String[]{"Brazilian Real", "R$"});
        currencies.put("MXN", new String[]{"Mexican Peso", "$"});
        currencies.put("ARS", new String[]{"Argentine Peso", "$"});
        currencies.put("CLP", new String[]{"Chilean Peso", "$"});
        currencies.put("COP", new String[]{"Colombian Peso", "$"});
        currencies.put("PEN", new String[]{"Peruvian Sol", "S/"});

        // Africa & Oceania
        currencies.put("ZAR", new String[]{"South African Rand", "R"});
        currencies.put("EGP", new String[]{"Egyptian Pound", "£"});
        currencies.put("NGN", new String[]{"Nigerian Naira", "₦"});
        currencies.put("KES", new String[]{"Kenyan Shilling", "Sh"});
        currencies.put("MAD", new String[]{"Moroccan Dirham", "د.م."});
        currencies.put("NZD", new String[]{"New Zealand Dollar", "NZ$"});
        currencies.put("FJD", new String[]{"Fijian Dollar", "FJ$"});

        log.info("Initializing {} currencies", currencies.size());
        for (Map.Entry<String, String[]> entry : currencies.entrySet()) {
            String code = entry.getKey();
            String[] details = entry.getValue();

            if (currencyRepository.findByCode(code).isEmpty()) {
                try {
                    CurrencyCreationRequest request = CurrencyCreationRequest.builder()
                            .code(code)
                            .name(details[0])
                            .symbol(details[1])
                            .build();
                    currencyService.createCurrency(request);
                    log.info("Created currency: {}", code);
                } catch (Exception e) {
                    log.error("Failed to create currency {}: {}", code, e.getMessage());
                }
            } else {
                log.info("Currency {} already exists", code);
            }
        }
    }
    private void initCategories() {
        // Danh mục cho GROUP
        initCategoryIfNotExists("Du lịch & Khám phá", "Cho các chuyến du lịch, phiêu lưu", 
                               Category.CategoryType.GROUP, "travel_explore", "#4285F4", true);
        initCategoryIfNotExists("Ăn uống & Gặp mặt", "Cho các bữa ăn chung, cà phê, tiệc", 
                               Category.CategoryType.GROUP, "restaurant", "#EA4335", true);
        initCategoryIfNotExists("Sinh hoạt chung", "Chi phí sinh hoạt, chi tiêu chung trong nhà", 
                               Category.CategoryType.GROUP, "home", "#34A853", true);
        initCategoryIfNotExists("Sự kiện & Lễ kỷ niệm", "Sinh nhật, tiệc tùng, kỷ niệm", 
                               Category.CategoryType.GROUP, "celebration", "#FBBC05", true);
        initCategoryIfNotExists("Dự án & Công việc", "Các dự án hợp tác, chi phí công việc", 
                               Category.CategoryType.GROUP, "work", "#673AB7", true);
    
        // Danh mục cho EXPENSE
        initCategoryIfNotExists("Đi lại & Phương tiện", "Taxi, xăng xe, vé tàu xe", 
                               Category.CategoryType.EXPENSE, "directions_car", "#FF9800", true);
        initCategoryIfNotExists("Ăn uống", "Nhà hàng, đồ ăn, đồ uống", 
                               Category.CategoryType.EXPENSE, "restaurant_menu", "#F44336", true);
        initCategoryIfNotExists("Chỗ ở & Lưu trú", "Khách sạn, homestay, airbnb", 
                               Category.CategoryType.EXPENSE, "hotel", "#2196F3", true);
        initCategoryIfNotExists("Mua sắm", "Quần áo, đồ dùng, quà tặng", 
                               Category.CategoryType.EXPENSE, "shopping_bag", "#E91E63", true);
        initCategoryIfNotExists("Giải trí", "Vé xem phim, công viên, hoạt động giải trí", 
                               Category.CategoryType.EXPENSE, "local_activity", "#9C27B0", true);
    
        // Danh mục BOTH
        initCategoryIfNotExists("Khác", "Các chi phí không thuộc danh mục nào", 
                               Category.CategoryType.BOTH, "more_horiz", "#9E9E9E", true);
    }
    
    private void initCategoryIfNotExists(String name, String description, 
            Category.CategoryType type, String iconCode, String color, boolean isSystem) {
        if (categoryRepository.findByNameAndType(name, type).isEmpty()) {
            try {
                Category category = new Category();
                category.setName(name);
                category.setDescription(description);
                category.setType(type);
                category.setIconCode(iconCode);
                category.setColor(color);
                category.setIsSystemCategory(isSystem);
                categoryRepository.save(category);
                log.info("✅ Created category: {} ({})", name, type);
            } catch (Exception e) {
                log.error("❌ Failed to create category {}: {}", name, e.getMessage());
            }
        }
    }
}
