package com.prephub.security;

import com.prephub.user.User;
import com.prephub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) {
        User u = users.findByUsernameIgnoreCaseOrEmailIgnoreCase(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));
        return new AppUserPrincipal(u.getId(), u.getUsername(), u.getEmail(),
                u.getPasswordHash(), u.getRole(), u.isEnabled());
    }
}
