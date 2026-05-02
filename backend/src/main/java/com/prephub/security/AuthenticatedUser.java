package com.prephub.security;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String username, String role) {}
