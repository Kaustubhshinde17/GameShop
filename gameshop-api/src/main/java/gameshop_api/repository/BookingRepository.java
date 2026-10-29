package gameshop_api.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import gameshop_api.model.Booking;
import gameshop_api.model.BookingStatus;

@Repository
public class BookingRepository {

    private final JdbcTemplate jdbcTemplate;

    public BookingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Booking> findAll() {
        String sql = """
                SELECT booking_id, customer_name, start_time, end_time,
                       duration_hours, hourly_rate, status, assigned_station_id
                FROM bookings
                ORDER BY booking_id
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new Booking(
                rs.getInt("booking_id"),
                rs.getString("customer_name"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime(),
                rs.getInt("duration_hours"),
                rs.getBigDecimal("hourly_rate"),
                BookingStatus.valueOf(rs.getString("status")),
                (Integer) rs.getObject("assigned_station_id")
        ));
    }

    public boolean existsById(int bookingId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE booking_id = ?",
                Integer.class,
                bookingId
        );
        return count != null && count > 0;
    }

    public boolean save(Booking booking) {
        String sql = """
                INSERT INTO bookings
                    (customer_name, start_time, end_time, duration_hours,
                     hourly_rate, status, assigned_station_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        int rows = jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, booking.getCustomerName());
            statement.setObject(2, booking.getStartTime());
            statement.setObject(3, booking.getEndTime());
            statement.setInt(4, booking.getDurationHours());
            statement.setBigDecimal(5, booking.getHourlyRate());
            statement.setString(6, booking.getStatus().name());
            if (booking.getAssignedStationId() == null) {
                statement.setNull(7, java.sql.Types.INTEGER);
            } else {
                statement.setInt(7, booking.getAssignedStationId());
            }
            return statement;
        }, keyHolder);

        if (rows == 1) {
            Number generatedKey = keyHolder.getKey();

            if (generatedKey != null) {
                booking.setBookingId(generatedKey.intValue());
                return true;
            }
        }

        return false;
    }

    public boolean update(Booking booking) {
        String sql = """
                UPDATE bookings
                SET customer_name = ?, start_time = ?, end_time = ?,
                    duration_hours = ?, hourly_rate = ?, status = ?,
                    assigned_station_id = ?
                WHERE booking_id = ?
                """;

        int rows = jdbcTemplate.update(
                sql,
                booking.getCustomerName(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getDurationHours(),
                booking.getHourlyRate(),
                booking.getStatus().name(),
                booking.getAssignedStationId(),
                booking.getBookingId()
        );
        return rows == 1;
    }
}
