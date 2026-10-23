package com.pharma.erp.clients;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClientFeedbackRepository extends JpaRepository<ClientFeedback, Integer> {

    Page<ClientFeedback> findByClientId(Integer clientId, Pageable pageable);

    long countByClientId(Integer clientId);

    @Query("select avg(f.rating) from ClientFeedback f where f.clientId = :clientId and f.rating is not null")
    Double averageRating(Integer clientId);
}
