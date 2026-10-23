package com.pharma.erp.finance;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/** Inserts rows owned by other modules directly, since finance references them by id only. */
final class FinanceTestData {

    private final JdbcTemplate jdbc;

    FinanceTestData(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    int client(String name) {
        return insert("INSERT INTO `client` (`client_name`) VALUES (?)", name);
    }

    int store() {
        return insert("INSERT INTO `store` (`store_name`) VALUES (?)", "Finance IT store " + UUID.randomUUID());
    }

    int item(String name) {
        return insert("INSERT INTO `inventory_item` (`item_name`) VALUES (?)", name);
    }

    private int insert(String sql, String value) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, value);
            return ps;
        }, keys);
        return Objects.requireNonNull(keys.getKey()).intValue();
    }
}
