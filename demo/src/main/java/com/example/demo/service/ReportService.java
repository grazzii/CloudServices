package com.example.demo.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {
    private final JdbcTemplate jdbc;
    public ReportService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String, Object> buildStats() {
        // AJUSTE o SQL para suas tabelas reais
        Integer users = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        Integer orders = jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class);

        Map<String, Integer> ordersByStatus = jdbc.query(
            "SELECT status, COUNT(*) c FROM orders GROUP BY status",
            rs -> {
                Map<String, Integer> m = new HashMap<>();
                while (rs.next()) m.put(rs.getString("status"), rs.getInt("c"));
                return m;
            });

        return Map.of(
            "usersCount", users,
            "ordersCount", orders,
            "ordersByStatus", ordersByStatus
        );
    }
}
