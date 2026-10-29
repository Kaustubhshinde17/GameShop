
package gameshop_api.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Booking {

    private int bookingId;
    private String customerName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int durationHours;
    private BigDecimal hourlyRate;
    private BookingStatus status;
    private Integer assignedStationId;

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

    public BigDecimal getTotalCost() {
        return hourlyRate.multiply(BigDecimal.valueOf(durationHours));
    }
    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }
}
