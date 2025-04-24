package com.TravelShare.configuration;

import com.TravelShare.dto.request.CurrencyCreationRequest;
import com.TravelShare.dto.request.ExpenseCategoryCreationRequest;
import com.TravelShare.entity.User;
import com.TravelShare.repository.CurrencyRepository;
import com.TravelShare.repository.ExpenseCategoryRepository;
import com.TravelShare.repository.UserRepository;
import com.TravelShare.service.CurrencyService;
import com.TravelShare.service.ExpenseCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrencyRepository currencyRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final CurrencyService currencyService;
    private final ExpenseCategoryService expenseCategoryService;

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
        initExpenseCategories();
        initRegularUsers();
    }
    private void initRegularUsers() {
        createUserIfNotExists("lam123", "123456", "lam@example.com", "Lam Le");
        createUserIfNotExists("liem456", "123456", "liem@example.com", "Liem Le");
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
                    .trips(new HashSet<>())
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
                    .trips(new HashSet<>())
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
        // Define common currencies
        Map<String, String[]> currencies = Map.of(
                "USD", new String[]{"US Dollar", "$"},
                "EUR", new String[]{"Euro", "€"},
                "GBP", new String[]{"British Pound", "£"},
                "JPY", new String[]{"Japanese Yen", "¥"},
                "VND", new String[]{"Vietnamese Dong", "₫"},
                "CNY", new String[]{"Chinese Yuan", "¥"},
                "AUD", new String[]{"Australian Dollar", "A$"},
                "CAD", new String[]{"Canadian Dollar", "C$"}
        );

        log.info("Initializing currencies");
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

    private void initExpenseCategories() {
        // Define common expense categories
        Map<String, String> categories = Map.of(
                "Food", "Expenses for meals, restaurants, groceries",
                "Transportation", "Expenses for taxi, train, bus, flights, fuel",
                "Accommodation", "Hotel, AirBnB, hostel expenses",
                "Activities", "Tours, tickets, entrance fees",
                "Shopping", "Souvenirs, clothes, personal items",
                "Other", "Miscellaneous expenses"
        );

        log.info("Initializing expense categories");
        for (Map.Entry<String, String> entry : categories.entrySet()) {
            String name = entry.getKey();
            String description = entry.getValue();

            if (expenseCategoryRepository.findByName(name).isEmpty()) {
                try {
                    ExpenseCategoryCreationRequest request = ExpenseCategoryCreationRequest.builder()
                            .name(name)
                            .description(description)
                            .build();
                    expenseCategoryService.createCategory(request);
                    log.info("Created expense category: {}", name);
                } catch (Exception e) {
                    log.error("Failed to create expense category {}: {}", name, e.getMessage());
                }
            } else {
                log.info("Expense category {} already exists", name);
            }
        }
    }
}
