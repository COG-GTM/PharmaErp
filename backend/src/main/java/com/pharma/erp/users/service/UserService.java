package com.pharma.erp.users.service;

import com.pharma.erp.common.web.ConflictException;
import com.pharma.erp.common.web.NotFoundException;
import com.pharma.erp.users.dto.RegisterUserRequest;
import com.pharma.erp.users.dto.UserResponse;
import com.pharma.erp.users.entity.UserAccount;
import com.pharma.erp.users.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and administration of rows in the legacy {@code user} table. Passwords are stored
 * as BCrypt hashes; the encoder is private to this module so it does not clash with an auth bean.
 */
@Service
public class UserService {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserAccountRepository users) {
        this.users = users;
    }

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        if (users.existsById(request.username())) {
            throw duplicate(request.username());
        }
        try {
            UserAccount saved = users.saveAndFlush(
                    new UserAccount(request.username(), passwordEncoder.encode(request.password())));
            return UserResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw duplicate(request.username());
        }
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return users.findAll(pageable).map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse get(String username) {
        return UserResponse.from(find(username));
    }

    @Transactional
    public void changePassword(String username, String newPassword) {
        find(username).setPasswordHash(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void delete(String username, String currentUsername) {
        UserAccount user = find(username);
        if (user.getUsername().equals(currentUsername)) {
            throw new ConflictException("You cannot delete the account you are signed in with");
        }
        users.delete(user);
    }

    private UserAccount find(String username) {
        return users.findById(username).orElseThrow(() -> NotFoundException.of("User", username));
    }

    private static ConflictException duplicate(String username) {
        return new ConflictException("User " + username + " already exists");
    }
}
