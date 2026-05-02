package com.prephub.user;

import com.prephub.common.NotFoundException;
import com.prephub.config.AppProperties;
import com.prephub.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;
    private final PortfolioRepository portfolios;
    private final StorageService storage;
    private final AppProperties props;

    @Cacheable(value = "userProfile", key = "#username")
    @Transactional(readOnly = true)
    public UserDtos.UserProfile getByUsername(String username) {
        User u = users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Portfolio p = portfolios.findById(u.getId()).orElseGet(() -> Portfolio.builder().userId(u.getId()).build());
        return toProfile(u, p);
    }

    @Transactional
    @CacheEvict(value = "userProfile", allEntries = true)
    public UserDtos.UserProfile updateProfile(UUID userId, UserDtos.UpdateProfileRequest req) {
        User u = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (req.displayName() != null) u.setDisplayName(req.displayName());
        if (req.bio() != null) u.setBio(req.bio());
        users.save(u);
        Portfolio p = portfolios.findById(userId).orElseGet(() -> Portfolio.builder().userId(userId).build());
        return toProfile(u, p);
    }

    @Transactional
    @CacheEvict(value = "userProfile", allEntries = true)
    public String uploadAvatar(UUID userId, MultipartFile file) throws Exception {
        User u = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (u.getAvatarKey() != null)
            storage.delete(props.storage().avatarBucket(), u.getAvatarKey());
        String key = storage.uploadAvatar(userId, file);
        u.setAvatarKey(key);
        users.save(u);
        return storage.publicUrl(props.storage().avatarBucket(), key);
    }

    private UserDtos.UserProfile toProfile(User u, Portfolio p) {
        return new UserDtos.UserProfile(
                u.getId(), u.getUsername(), u.getEmail(), u.getDisplayName(), u.getBio(),
                u.getAvatarKey() != null ? storage.publicUrl(props.storage().avatarBucket(), u.getAvatarKey()) : null,
                u.getRole(), u.getCreatedAt(), UserDtos.PortfolioStats.of(p));
    }
}
