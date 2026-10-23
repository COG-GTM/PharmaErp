package com.pharma.erp.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

/** Legacy {@code user} table: the username is the assigned primary key; {@code password} holds a BCrypt hash. */
@Entity(name = "UserAccount")
@Table(name = "`user`")
public class UserAccount implements Persistable<String> {

    @Id
    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Transient
    private boolean isNew = true;

    protected UserAccount() {}

    public UserAccount(String username, String passwordHash) {
        this.username = username;
        this.password = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return password;
    }

    public void setPasswordHash(String passwordHash) {
        this.password = passwordHash;
    }

    @Override
    public String getId() {
        return username;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
