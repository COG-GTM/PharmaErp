package com.pharma.erp.staff.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Spec sheet "LogIn Type" (jr.Employee, sr.Employee, Manager). */
@Entity
@Table(name = "login_type")
public class LoginType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_id")
    private Integer id;

    @Column(name = "login_name", nullable = false, length = 100)
    private String name;

    protected LoginType() {
    }

    public LoginType(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
