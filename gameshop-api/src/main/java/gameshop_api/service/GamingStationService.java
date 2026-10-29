
package gameshop_api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import gameshop_api.model.Booking;
import gameshop_api.model.BookingStatus;
import gameshop_api.model.GamingStation;
import gameshop_api.repository.BookingRepository;
import gameshop_api.repository.GamingStationRepository;

@Service
public class GamingStationService {

    private final GamingStationRepository stationRepository;
    private final BookingRepository bookingRepository;

    public GamingStationService(
            GamingStationRepository stationRepository,
            BookingRepository bookingRepository) {
        this.stationRepository = stationRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<GamingStation> getAllStations() {
        return stationRepository.findAll();
    }

    public List<GamingStation> getAvailableStations(
            LocalDateTime startTime,
            int durationHours) {

        // Validate the requested time slot.
        if (startTime == null) {
            throw new IllegalArgumentException(
                    "Start time is required");
        }

        if (durationHours <= 0) {
            throw new IllegalArgumentException(
                    "Duration must be greater than zero");
        }

        LocalDateTime endTime = startTime.plusHours(durationHours);

        // Café opening hours: 10:00 AM to 11:00 PM.
        LocalDateTime openingTime =
                startTime.toLocalDate().atTime(10, 0);

        LocalDateTime closingTime =
                startTime.toLocalDate().atTime(23, 0);

        if (startTime.isBefore(openingTime)
                || endTime.isAfter(closingTime)) {
            throw new IllegalArgumentException(
                    "Booking must be within café opening hours (10 AM - 11 PM)");
        }

        // Fetch consoles and existing bookings from MySQL.
        List<GamingStation> stations =
                stationRepository.findAll();

        List<Booking> bookings =
                bookingRepository.findAll();

        // Keep operational consoles with no conflicting
        // confirmed booking.
        return stations.stream()
                .filter(GamingStation::isOperational)
                .filter(station -> bookings.stream().noneMatch(booking -> {

                    if (booking.getStatus()
                            != BookingStatus.CONFIRMED) {
                        return false;
                    }

                    Integer assignedStationId =
                            booking.getAssignedStationId();

                    if (assignedStationId == null
                            || assignedStationId.intValue()
                                    != station.getStationId()) {
                        return false;
                    }

                    // Two bookings overlap if each starts
                    // before the other one ends.
                    return startTime.isBefore(booking.getEndTime())
                            && endTime.isAfter(booking.getStartTime());
                }))
                .toList();
    }
}
