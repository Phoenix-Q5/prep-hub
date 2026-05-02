package com.prephub.user;

import com.prephub.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;
    private final CurrentUser currentUser;

    @GetMapping("/{username}")
    public UserDtos.UserProfile get(@PathVariable String username) {
        return service.getByUsername(username);
    }

    @GetMapping("/me")
    public UserDtos.UserProfile me() {
        return service.getByUsername(currentUser.getRequired().username());
    }

    @PatchMapping("/me")
    public UserDtos.UserProfile updateMe(@Valid @RequestBody UserDtos.UpdateProfileRequest req) {
        return service.updateProfile(currentUser.getRequired().id(), req);
    }

    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) throws Exception {
        String url = service.uploadAvatar(currentUser.getRequired().id(), file);
        return ResponseEntity.ok(Map.of("avatarUrl", url));
    }
}
