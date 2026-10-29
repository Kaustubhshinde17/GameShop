
package gameshop_api.controller;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin/stations")
public class AdminStationController {

    private final JdbcTemplate jdbcTemplate;

    public AdminStationController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Map<String, Object>> listStations() {
        return jdbcTemplate.queryForList(
            "SELECT station_id AS stationId, station_name AS stationName, " +
            "hourly_rate AS hourlyRate, operational " +
            "FROM gaming_stations ORDER BY station_id"
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createStation(
            @RequestBody Map<String, Object> request) {

        String name = request.get("stationName") == null
                ? ""
                : request.get("stationName").toString().trim();

        BigDecimal rate = parseRate(request.get("hourlyRate"));

        if (name.length() < 2 || name.length() > 50) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Station name must be between 2 and 50 characters."
            );
        }

        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Hourly rate must be greater than zero."
            );
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO gaming_stations " +
                "(station_name, hourly_rate, operational) " +
                "VALUES (?, ?, TRUE)",
                Statement.RETURN_GENERATED_KEYS
            );

            ps.setString(1, name);
            ps.setBigDecimal(2, rate);

            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();

        return Map.of(
            "stationId", key == null ? 0 : key.intValue(),
            "stationName", name,
            "hourlyRate", rate,
            "operational", true
        );
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateStation(
            @PathVariable int id,
            @RequestBody Map<String, Object> request) {

        String name = request.get("stationName") == null
                ? ""
                : request.get("stationName").toString().trim();

        BigDecimal rate = parseRate(request.get("hourlyRate"));
        Object operationalValue = request.get("operational");

        if (name.length() < 2 || name.length() > 50) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Station name must be between 2 and 50 characters."
            );
        }

        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Hourly rate must be greater than zero."
            );
        }

        if (!(operationalValue instanceof Boolean operational)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Operational must be true or false."
            );
        }

        int changed = jdbcTemplate.update(
            "UPDATE gaming_stations " +
            "SET station_name = ?, hourly_rate = ?, operational = ? " +
            "WHERE station_id = ?",
            name, rate, operational, id
        );

        if (changed == 0) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Station not found."
            );
        }

        return Map.of(
            "stationId", id,
            "stationName", name,
            "hourlyRate", rate,
            "operational", operational
        );
    }

    private BigDecimal parseRate(Object value) {
        try {
            return new BigDecimal(value.toString());
        } catch (Exception exception) {
            return null;
        }
    }
    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, Object> deleteStation(@PathVariable int id) {

        Integer linkedBookings = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM bookings WHERE assigned_station_id = ?",
            Integer.class,
            id
        );

        if (linkedBookings != null && linkedBookings > 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "This console has linked bookings. Disable it instead to preserve booking history."
            );
        }

        int deleted = jdbcTemplate.update(
            "DELETE FROM gaming_stations WHERE station_id = ?",
            id
        );

        if (deleted == 0) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Console not found."
            );
        }

        return Map.of(
            "message", "Console deleted successfully.",
            "stationId", id
        );
    }
}
