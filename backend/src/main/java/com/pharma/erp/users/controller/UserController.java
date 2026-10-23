package com.pharma.erp.users.controller;

import com.pharma.erp.common.web.PageResponse;
import com.pharma.erp.users.dto.ChangePasswordRequest;
import com.pharma.erp.users.dto.RegisterUserRequest;
import com.pharma.erp.users.dto.UserResponse;
import com.pharma.erp.users.service.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/** Use case "Register" plus admin management of system users. Requires the {@code admin} scope. */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAuthority('SCOPE_admin')")
public class UserController {

    private static final Set<String> SORTABLE = Set.of("username");

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterUserRequest request, UriComponentsBuilder uri) {
        UserResponse created = userService.register(request);
        return ResponseEntity.created(uri.path("/api/users/{username}").buildAndExpand(created.username()).toUri())
                .body(created);
    }

    @GetMapping
    public PageResponse<UserResponse> list(
            @PageableDefault(size = 20, sort = "username", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(userService.list(SortGuard.check(pageable, SORTABLE)));
    }

    @GetMapping("/{username}")
    public UserResponse get(@PathVariable String username) {
        return userService.get(username);
    }

    @PutMapping("/{username}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable String username, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(username, request.password());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> delete(@PathVariable String username, Principal principal) {
        userService.delete(username, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
