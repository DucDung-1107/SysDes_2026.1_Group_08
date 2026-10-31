package vn.medora.api.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class HealthController {
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    public HealthController(org.springframework.jdbc.core.JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/health")
    public  String health() {
        return "UP";
    }

    @GetMapping("/ready")
    public org.springframework.http.ResponseEntity<String> ready() {
        try {
            jdbc.queryForObject("select 1", Integer.class);
            return org.springframework.http.ResponseEntity.ok("UP");
        } catch (org.springframework.dao.DataAccessException exception) {
            return org.springframework.http.ResponseEntity.status(503).body("DOWN");
        }
    }
}
