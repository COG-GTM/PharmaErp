package com.pharma.erp.auth;

import com.pharma.erp.common.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** JSON authentication API for the SPA. All routes are public at the filter level (/api/auth/**). */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final AuthService authService;
    private final TokenCookies cookies;
    private final JwtService jwtService;

    AuthController(AuthService authService, TokenCookies cookies, JwtService jwtService) {
        this.authService = authService;
        this.cookies = cookies;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        IssuedToken token = authService
                .login(request.username(), request.password())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.issue(token).toString())
                .body(LoginResponse.from(token));
    }

    @GetMapping("/me")
    MeResponse me() {
        CurrentToken current = CurrentToken.require();
        return new MeResponse(
                current.username(),
                current.scope(),
                jwtService.parse(current.token()).getExpiration().toInstant());
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .build();
    }
}
