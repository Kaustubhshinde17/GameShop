
package gameshop_api.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Booking {

    private int bookingId;
    private String customerName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int durationHours;
    private BigDecimal hourlyRate;
    private BookingStatus status;

    // Legacy field for existing single-console bookings.
    private Integer assignedStationId;

    // New field for multiple consoles.
    private List<BookingStation> bookingStations = new ArrayList<>();

    public Booking(int bookingId, String customerName,
                   LocalDateTime startTime, LocalDateTime endTime,
                   int durationHours, BigDecimal hourlyRate,
                   BookingStatus status, Integer assignedStationId) {

        this.bookingId = bookingId;
        this.customerName = customerName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationHours = durationHours;
        this.hourlyRate = hourlyRate;
        this.status = status;
        this.assignedStationId = assignedStationId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public int getDurationHours() {
        return durationHours;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Integer getAssignedStationId() {
        return assignedStationId;
    }

    public List<BookingStation> getBookingStations() {
        return bookingStations;
    }

    public void setBookingStations(List<BookingStation> bookingStations) {
        this.bookingStations = bookingStations == null
                ? new ArrayList<>()
                : new ArrayList<>(bookingStations);
    }

    public BigDecimal getTotalCost() {
        return hourlyRate.multiply(BigDecimal.valueOf(durationHours));
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }
}
