package com.prephub.bootstrap;

import com.prephub.common.Role;
import com.prephub.user.Portfolio;
import com.prephub.user.PortfolioRepository;
import com.prephub.user.User;
import com.prephub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder {

    private final UserRepository users;
    private final PortfolioRepository portfolios;
    private final PasswordEncoder encoder;

    @Value("${app.bootstrap.admin-username:admin}")
    private String adminUsername;
    @Value("${app.bootstrap.admin-email:admin@prephub.local}")
    private String adminEmail;
    @Value("${app.bootstrap.admin-password:admin123}")
    private String adminPassword;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (users.existsByUsernameIgnoreCase(adminUsername)) return;
        User admin = User.builder()
                .username(adminUsername).email(adminEmail)
                .passwordHash(encoder.encode(adminPassword))
                .displayName("Admin").role(Role.ADMIN).enabled(true).build();
        users.save(admin);
        portfolios.save(Portfolio.builder().userId(admin.getId()).user(admin).build());
        log.warn("Bootstrapped admin user '{}' with password '{}' — change immediately in production",
                adminUsername, adminPassword);
    }
}
