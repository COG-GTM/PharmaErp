package com.pharma.erp.auth;

import java.time.Instant;

public record MeResponse(String username, String scope, Instant expiresAt) {}
