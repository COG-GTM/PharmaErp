package com.pharma.erp.clients;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Feedback left by a client (spec entity Feedback). {@code employee_id} is kept as a plain column so
 * this module does not depend on the staff module's entity.
 */
@Entity
@Table(name = "feedback")
public class ClientFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Integer id;

    @Column(name = "client_id")
    private Integer clientId;

    @Column(name = "employee_id")
    private Integer employeeId;

    @Column(name = "feedback_description", nullable = false, length = 2000)
    private String comment;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "rating")
    private Integer rating;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ClientFeedback() {
    }

    public ClientFeedback(Integer clientId, Integer employeeId, Integer rating, String comment) {
        this.clientId = clientId;
        this.employeeId = employeeId;
        this.rating = rating;
        this.comment = comment;
    }

    public Integer getId() {
        return id;
    }

    public Integer getClientId() {
        return clientId;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public String getComment() {
        return comment;
    }

    public Integer getRating() {
        return rating;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
