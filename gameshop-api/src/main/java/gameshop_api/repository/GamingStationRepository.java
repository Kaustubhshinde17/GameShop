
package gameshop_api.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import gameshop_api.model.GamingStation;

@Repository
public class GamingStationRepository {

    private final JdbcTemplate jdbcTemplate;

    public GamingStationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<GamingStation> findAll() {
        String sql = """
                SELECT station_id, station_name,
                       hourly_rate, operational
                FROM gaming_stations
                ORDER BY station_id
                """;

        RowMapper<GamingStation> rowMapper = (ResultSet rs, int rowNum)
                -> new GamingStation(
                        rs.getInt("station_id"),
                        rs.getString("station_name"),
                        rs.getBigDecimal("hourly_rate"),
                        rs.getBoolean("operational")
                );

        return jdbcTemplate.query(sql, rowMapper);
    }
    public List<GamingStation> findAllForUpdate() {
        String sql = """
                SELECT station_id, station_name,
                       hourly_rate, operational
                FROM gaming_stations
                ORDER BY station_id
                FOR UPDATE
                """;

        RowMapper<GamingStation> rowMapper = (ResultSet rs, int rowNum)
                -> new GamingStation(
                        rs.getInt("station_id"),
                        rs.getString("station_name"),
                        rs.getBigDecimal("hourly_rate"),
                        rs.getBoolean("operational")
                );

        return jdbcTemplate.query(sql, rowMapper);
    }
}
