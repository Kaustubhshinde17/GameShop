
package gameshop_api.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gameshop_api.dto.CreateBookingRequest;
import gameshop_api.model.Booking;
import gameshop_api.model.BookingStation;
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

    // Create a booking with multiple selected consoles.
    @Transactional
    public Booking createBooking(CreateBookingRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Booking request cannot be empty");
        }

        String customerName = request.getCustomerName();
        LocalDateTime startTime = request.getStartTime();
        int durationHours = request.getDurationHours();
        List<Integer> stationIds = request.getStationIds();

        // Validate customer name.
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException(
                    "Customer name cannot be empty");
        }

        customerName = customerName.trim();

        if (customerName.length() < 2 || customerName.length() > 100) {
            throw new IllegalArgumentException(
                    "Customer name must be between 2 and 100 characters");
        }

        // Validate start time.
        if (startTime == null) {
            throw new IllegalArgumentException(
                    "Start time cannot be null");
        }

        // Reject bookings in the past using Indian Standard Time.
        LocalDateTime now = LocalDateTime.now(
                ZoneId.of("Asia/Kolkata"));

        if (startTime.isBefore(now)) {
            throw new IllegalArgumentException(
                    "Cannot book a session in the past");
        }

        // Validate duration.
        if (durationHours <= 0) {
            throw new IllegalArgumentException(
                    "Duration must be greater than zero");
        }

        // Validate selected consoles.
        if (stationIds == null || stationIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Select at least one console");
        }

        if (stationIds.size() > 4) {
            throw new IllegalArgumentException(
                    "You can select a maximum of four consoles");
        }

        Set<Integer> uniqueIds = new HashSet<>(stationIds);

        if (uniqueIds.size() != stationIds.size()) {
            throw new IllegalArgumentException(
                    "Duplicate console selections are not allowed");
        }

        // Validate café operating hours.
        LocalDateTime endTime = startTime.plusHours(durationHours);

        LocalDateTime openingTime =
                startTime.toLocalDate().atTime(10, 0);

        LocalDateTime closingTime =
                startTime.toLocalDate().atTime(23, 0);

        if (startTime.isBefore(openingTime)
                || endTime.isAfter(closingTime)) {

            throw new IllegalArgumentException(
                    "Booking must be within café opening hours (10 AM - 11 PM)");
        }

        // Lock station records to prevent simultaneous reservations.
        List<GamingStation> stations =
                stationRepository.findAllForUpdate();

        // Fetch existing bookings.
        List<Booking> bookings = bookingRepository.findAll();

        BigDecimal combinedHourlyRate = BigDecimal.ZERO;

        // Store selected consoles with their booking-time prices.
        List<BookingStation> selectedStations = new ArrayList<>();

        for (Integer stationId : stationIds) {

            GamingStation station = stations.stream()
                    .filter(s -> s.getStationId() == stationId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Console not found: " + stationId));

            if (!station.isOperational()) {
                throw new IllegalStateException(
                        station.getStationName() + " is not operational");
            }

            // Check whether this console is already reserved.
            boolean occupied = bookings.stream().anyMatch(existing -> {

                if (existing.getStatus() != BookingStatus.PENDING
                        && existing.getStatus() != BookingStatus.CONFIRMED) {
                    return false;
                }

                boolean selectedConsole = existing.getBookingStations()
                        .stream()
                        .anyMatch(bs -> bs.getStationId() == stationId);

                boolean overlaps =
                        startTime.isBefore(existing.getEndTime())
                        && endTime.isAfter(existing.getStartTime());

                return selectedConsole && overlaps;
            });

            if (occupied) {
                throw new IllegalStateException(
                        station.getStationName()
                                + " is already reserved for this time slot");
            }

            BigDecimal stationRate = station.getHourlyRate();

            combinedHourlyRate =
                    combinedHourlyRate.add(stationRate);

            selectedStations.add(
                    new BookingStation(stationId, stationRate));
        }

        // Create the pending booking.
        Booking booking = new Booking(
                0,
                customerName,
                startTime,
                endTime,
                durationHours,
                combinedHourlyRate,
                BookingStatus.PENDING,
                null
        );

        booking.setBookingStations(selectedStations);

        if (!bookingRepository.save(booking)) {
            throw new IllegalStateException(
                    "Booking could not be saved");
        }

        return booking;
    }

    // Confirm the same consoles selected by the customer.
    @Transactional
    public Booking confirmBooking(int bookingId) {

        List<GamingStation> stations =
                stationRepository.findAllForUpdate();

        List<Booking> bookings = bookingRepository.findAll();

        Booking booking = bookings.stream()
                .filter(b -> b.getBookingId() == bookingId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending bookings can be confirmed");
        }

        List<BookingStation> selectedStations =
                booking.getBookingStations();

        // Support older pending bookings created before multi-console selection.
        if (selectedStations.isEmpty()) {

            GamingStation availableStation = stations.stream()
                    .filter(GamingStation::isOperational)
                    .filter(station -> bookings.stream().noneMatch(existing -> {

                        if (existing.getBookingId() == bookingId
                                || (existing.getStatus() != BookingStatus.PENDING
                                && existing.getStatus() != BookingStatus.CONFIRMED)) {
                            return false;
                        }

                        boolean selectedConsole = existing.getBookingStations()
                                .stream()
                                .anyMatch(bs ->
                                        bs.getStationId() == station.getStationId());

                        boolean overlaps =
                                booking.getStartTime().isBefore(existing.getEndTime())
                                && booking.getEndTime().isAfter(existing.getStartTime());

                        return selectedConsole && overlaps;

                    }))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No console is available to confirm this booking"));

            selectedStations = List.of(
                    new BookingStation(
                            availableStation.getStationId(),
                            availableStation.getHourlyRate()));

            booking.setBookingStations(selectedStations);
        }

        // Verify every selected console is still operational and available.
        for (BookingStation selected : selectedStations) {

            GamingStation station = stations.stream()
                    .filter(s -> s.getStationId() == selected.getStationId())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Selected console no longer exists: "
                                    + selected.getStationId()));

            if (!station.isOperational()) {
                throw new IllegalStateException(
                        station.getStationName()
                                + " is no longer operational");
            }

            boolean occupied = bookings.stream().anyMatch(existing -> {

                if (existing.getBookingId() == bookingId
                        || (existing.getStatus() != BookingStatus.PENDING
                        && existing.getStatus() != BookingStatus.CONFIRMED)) {
                    return false;
                }

                boolean selectedConsole = existing.getBookingStations()
                        .stream()
                        .anyMatch(bs ->
                                bs.getStationId() == selected.getStationId());

                boolean overlaps =
                        booking.getStartTime().isBefore(existing.getEndTime())
                        && booking.getEndTime().isAfter(existing.getStartTime());

                return selectedConsole && overlaps;
            });

            if (occupied) {
                throw new IllegalStateException(
                        station.getStationName()
                                + " is no longer available");
            }
        }

        // Keep legacy assigned_station_id for single-console bookings.
        Integer assignedStationId = selectedStations.size() == 1
                ? selectedStations.get(0).getStationId()
                : null;

        Booking confirmedBooking = new Booking(
                booking.getBookingId(),
                booking.getCustomerName(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getDurationHours(),
                booking.getHourlyRate(),
                BookingStatus.CONFIRMED,
                assignedStationId
        );

        confirmedBooking.setBookingStations(selectedStations);

        if (!bookingRepository.update(confirmedBooking)) {
            throw new IllegalStateException(
                    "Booking could not be updated");
        }

        return confirmedBooking;
    }

    // Cancel a booking and release its consoles.
    @Transactional
    public Booking cancelBooking(int bookingId) {

        Booking booking = bookingRepository.findAll().stream()
                .filter(b -> b.getBookingId() == bookingId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Booking is already cancelled");
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

        // Preserve console history. CANCELLED status releases availability.
        cancelledBooking.setBookingStations(
                booking.getBookingStations());

        if (!bookingRepository.update(cancelledBooking)) {
            throw new IllegalStateException(
                    "Booking could not be cancelled");
        }

        return cancelledBooking;
    }
}
