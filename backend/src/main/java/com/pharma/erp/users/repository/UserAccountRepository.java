package com.pharma.erp.users.repository;

import com.pharma.erp.users.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {}
