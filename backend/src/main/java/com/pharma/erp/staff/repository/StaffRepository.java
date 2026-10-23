package com.pharma.erp.staff.repository;

import com.pharma.erp.staff.entity.Staff;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffRepository extends JpaRepository<Staff, Integer>, JpaSpecificationExecutor<Staff> {

    @EntityGraph(attributePaths = "loginType")
    Optional<Staff> findWithLoginTypeById(Integer id);

    boolean existsByLoginTypeId(Integer loginTypeId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Integer id);

    /** The store table belongs to the stores module; only its existence is checked here. */
    @Query(value = "SELECT COUNT(*) FROM store WHERE store_id = :storeId", nativeQuery = true)
    long countStores(@Param("storeId") Integer storeId);

    /** The legacy {@code user} table belongs to the users module; only its existence is checked here. */
    @Query(value = "SELECT COUNT(*) FROM `user` WHERE username = :username", nativeQuery = true)
    long countUsers(@Param("username") String username);
}
