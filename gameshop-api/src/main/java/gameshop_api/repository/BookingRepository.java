
package gameshop_api.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import gameshop_api.model.Booking;
import gameshop_api.model.BookingStation;
import gameshop_api.model.BookingStatus;

@Repository
public class BookingRepository {

    private final JdbcTemplate jdbcTemplate;

    public BookingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Retrieve all bookings with their selected consoles.
    public List<Booking> findAll() {

        String sql = """
                SELECT booking_id, customer_name, start_time, end_time,
                       duration_hours, hourly_rate, status,
                       assigned_station_id
                FROM bookings
                ORDER BY booking_id DESC
                """;

        List<Booking> bookings = jdbcTemplate.query(sql, (rs, rowNum) ->
                new Booking(
                        rs.getInt("booking_id"),
                        rs.getString("customer_name"),
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getTimestamp("end_time").toLocalDateTime(),
                        rs.getInt("duration_hours"),
                        rs.getBigDecimal("hourly_rate"),
                        BookingStatus.valueOf(rs.getString("status")),
                        (Integer) rs.getObject("assigned_station_id")
                )
        );

        // Load console relationships for each booking.
        for (Booking booking : bookings) {

            String stationSql = """
                    SELECT station_id, hourly_rate_at_booking
                    FROM booking_stations
                    WHERE booking_id = ?
                    ORDER BY station_id
                    """;

            List<BookingStation> bookingStations = jdbcTemplate.query(
                    stationSql,
                    (rs, rowNum) -> new BookingStation(
                            rs.getInt("station_id"),
                            rs.getBigDecimal("hourly_rate_at_booking")
                    ),
                    booking.getBookingId()
            );

            // Backward compatibility for older single-console bookings.
            if (bookingStations.isEmpty()
                    && booking.getAssignedStationId() != null) {

                bookingStations.add(
                        new BookingStation(
                                booking.getAssignedStationId(),
                                booking.getHourlyRate()
                        )
                );
            }

            booking.setBookingStations(bookingStations);
        }

        return bookings;
    }

    public boolean existsById(int bookingId) {

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE booking_id = ?",
                Integer.class,
                bookingId
        );

        return count != null && count > 0;
    }

    // Save a booking and all selected consoles.
    @Transactional
    public boolean save(Booking booking) {

        if (booking.getBookingStations() == null
                || booking.getBookingStations().isEmpty()) {

            throw new IllegalStateException(
                    "Cannot save booking without selected consoles."
            );
        }

        String sql = """
                INSERT INTO bookings
                    (customer_name, start_time, end_time,
                     duration_hours, hourly_rate, status,
                     assigned_station_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        int rows = jdbcTemplate.update(connection -> {

            PreparedStatement statement = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );

            statement.setString(1, booking.getCustomerName());
            statement.setObject(2, booking.getStartTime());
            statement.setObject(3, booking.getEndTime());
            statement.setInt(4, booking.getDurationHours());
            statement.setBigDecimal(5, booking.getHourlyRate());
            statement.setString(6, booking.getStatus().name());

            if (booking.getAssignedStationId() == null) {
                statement.setNull(7, Types.INTEGER);
            } else {
                statement.setInt(7, booking.getAssignedStationId());
            }

            return statement;

        }, keyHolder);

        if (rows != 1) {
            throw new IllegalStateException(
                    "Could not insert booking."
            );
        }

        Number generatedKey = keyHolder.getKey();

        if (generatedKey == null) {
            throw new IllegalStateException(
                    "Could not retrieve generated booking ID."
            );
        }

        booking.setBookingId(generatedKey.intValue());

        // Save all selected consoles.
        saveBookingStations(booking);

        return true;
    }

    // Insert console relationships into booking_stations.
    private void saveBookingStations(Booking booking) {

        String sql = """
                INSERT INTO booking_stations
                    (booking_id, station_id, hourly_rate_at_booking)
                VALUES (?, ?, ?)
                """;

        List<BookingStation> selectedStations =
                booking.getBookingStations();

        for (BookingStation station : selectedStations) {

            if (station == null || station.getHourlyRate() == null) {
                throw new IllegalStateException(
                        "Invalid console or hourly rate in booking."
                );
            }

            int rows = jdbcTemplate.update(
                    sql,
                    booking.getBookingId(),
                    station.getStationId(),
                    station.getHourlyRate()
            );

            if (rows != 1) {
                throw new IllegalStateException(
                        "Could not save console ID: "
                        + station.getStationId()
                );
            }
        }

        int savedCount = countBookingStations(booking.getBookingId());

        if (selectedStations.size() != savedCount) {
            throw new IllegalStateException(
                    "Not all selected consoles were saved."
            );
        }
    }

    private int countBookingStations(int bookingId) {

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM booking_stations
                WHERE booking_id = ?
                """,
                Integer.class,
                bookingId
        );

        return count == null ? 0 : count;
    }

    // Update booking status while preserving console relationships.
    @Transactional
    public boolean update(Booking booking) {

        String sql = """
                UPDATE bookings
                SET customer_name = ?,
                    start_time = ?,
                    end_time = ?,
                    duration_hours = ?,
                    hourly_rate = ?,
                    status = ?,
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

        if (rows != 1) {
            return false;
        }

        // Add relationships for older bookings if they are missing.
        if (countBookingStations(booking.getBookingId()) == 0
                && booking.getBookingStations() != null
                && !booking.getBookingStations().isEmpty()) {

            saveBookingStations(booking);
        }

        return true;
    }
}
