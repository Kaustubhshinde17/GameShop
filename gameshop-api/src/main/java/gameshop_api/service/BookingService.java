package gameshop_api.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gameshop_api.dto.CreateBookingRequest;
import gameshop_api.model.Booking;
import gameshop_api.model.BookingStatus;
import gameshop_api.model.GamingStation;
import gameshop_api.repository.BookingRepository;
import gameshop_api.repository.GamingStationRepository;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final GamingStationRepository stationRepository;

    public BookingService(
            BookingRepository bookingRepository,
            GamingStationRepository stationRepository) {
        this.bookingRepository = bookingRepository;
        this.stationRepository = stationRepository;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Transactional
  
    public Booking createBooking(CreateBookingRequest request) {
        List<GamingStation> stations = stationRepository.findAllForUpdate();
        if (request == null) {
            throw new IllegalArgumentException("Booking request cannot be empty");
        }

        String customerName = request.getCustomerName();
        LocalDateTime startTime = request.getStartTime();
        int durationHours = request.getDurationHours();

        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("Customer name cannot be empty");
        }
        customerName = customerName.trim();
        if (customerName.length() < 2 || customerName.length() > 100) {
            throw new IllegalArgumentException("Customer name must be between 2 and 100 characters");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        if (durationHours <= 0) {
            throw new IllegalArgumentException("Duration must be greater than zero");
        }

        LocalDateTime endTime = startTime.plusHours(durationHours);
        LocalDateTime openingTime = startTime.toLocalDate().atTime(10, 0);
        LocalDateTime closingTime = startTime.toLocalDate().atTime(23, 0);

        if (startTime.isBefore(openingTime) || endTime.isAfter(closingTime)) {
            throw new IllegalArgumentException(
                    "Booking must be within café opening hours (10 AM - 11 PM)");
        }

        List<Booking> bookings = bookingRepository.findAll();
        GamingStation availableStation = null;

        for (GamingStation station : stations) {
            if (!station.isOperational()) {
                continue;
            }

            boolean occupied = false;
            for (Booking existing : bookings) {
                if (existing.getStatus() != BookingStatus.CONFIRMED) {
                    continue;
                }

                Integer assignedId = existing.getAssignedStationId();
                if (assignedId == null || assignedId.intValue() != station.getStationId()) {
                    continue;
                }

                boolean overlaps = startTime.isBefore(existing.getEndTime())
                        && endTime.isAfter(existing.getStartTime());
                if (overlaps) {
                    occupied = true;
                    break;
                }
            }

            if (!occupied) {
                availableStation = station;
                break;
            }
        }

        if (availableStation == null) {
            throw new IllegalStateException("No console is available for this time slot");
        }

        BigDecimal hourlyRate = availableStation.getHourlyRate();
        Booking booking = new Booking(
                0,
                customerName,
                startTime,
                endTime,
                durationHours,
                hourlyRate,
                BookingStatus.PENDING,
                null
        );

        if (!bookingRepository.save(booking)) {
            throw new IllegalStateException("Booking could not be saved");
        }

        return booking;
    }

    @Transactional
    public Booking confirmBooking(int bookingId) {
        List<GamingStation> stations = stationRepository.findAllForUpdate();

        Booking booking = bookingRepository.findAll().stream()
                .filter(b -> b.getBookingId() == bookingId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only pending bookings can be confirmed");
        }

        List<Booking> bookings = bookingRepository.findAll();
        GamingStation availableStation = null;

        for (GamingStation station : stations) {
            if (!station.isOperational()) {
                continue;
            }

            boolean occupied = false;
            for (Booking existing : bookings) {
                if (existing.getBookingId() == bookingId
                        || existing.getStatus() != BookingStatus.CONFIRMED) {
                    continue;
                }

                Integer assignedId = existing.getAssignedStationId();
                if (assignedId == null || assignedId.intValue() != station.getStationId()) {
                    continue;
                }

                boolean overlaps = booking.getStartTime().isBefore(existing.getEndTime())
                        && booking.getEndTime().isAfter(existing.getStartTime());
                if (overlaps) {
                    occupied = true;
                    break;
                }
            }

            if (!occupied) {
                availableStation = station;
                break;
            }
        }

        if (availableStation == null) {
            throw new IllegalStateException("No console is available to confirm this booking");
        }

        Booking confirmedBooking = new Booking(
                booking.getBookingId(),
                booking.getCustomerName(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getDurationHours(),
                booking.getHourlyRate(),
                BookingStatus.CONFIRMED,
                availableStation.getStationId()
        );

        if (!bookingRepository.update(confirmedBooking)) {
            throw new IllegalStateException("Booking could not be updated");
        }

        return confirmedBooking;
    }

    @Transactional
    public Booking cancelBooking(int bookingId) {
        Booking booking = bookingRepository.findAll().stream()
                .filter(b -> b.getBookingId() == bookingId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }

        Booking cancelledBooking = new Booking(
                booking.getBookingId(),
                booking.getCustomerName(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getDurationHours(),
                booking.getHourlyRate(),
                BookingStatus.CANCELLED,
                null
        );

        if (!bookingRepository.update(cancelledBooking)) {
            throw new IllegalStateException("Booking could not be cancelled");
        }

        return cancelledBooking;
    }
}
