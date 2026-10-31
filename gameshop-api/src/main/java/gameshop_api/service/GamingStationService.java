
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

        if (startTime == null) {
            throw new IllegalArgumentException(
                    "Start time is required");
        }

        if (durationHours <= 0) {
            throw new IllegalArgumentException(
                    "Duration must be greater than zero");
        }

        LocalDateTime endTime =
                startTime.plusHours(durationHours);

        LocalDateTime openingTime =
                startTime.toLocalDate().atTime(10, 0);

        LocalDateTime closingTime =
                startTime.toLocalDate().atTime(23, 0);

        if (startTime.isBefore(openingTime)
                || endTime.isAfter(closingTime)) {
            throw new IllegalArgumentException(
                    "Booking must be within café opening hours (10 AM - 11 PM)");
        }

        List<GamingStation> stations =
                stationRepository.findAll();

        List<Booking> bookings =
                bookingRepository.findAll();

        return stations.stream()
                .filter(GamingStation::isOperational)
                .filter(station -> bookings.stream().noneMatch(booking -> {

                    if (booking.getStatus() != BookingStatus.PENDING
                            && booking.getStatus() != BookingStatus.CONFIRMED) {
                        return false;
                    }

                    boolean selectedConsole =
                            booking.getBookingStations().stream()
                                    .anyMatch(bs ->
                                            bs.getStationId() == station.getStationId());

                    boolean overlaps =
                            startTime.isBefore(booking.getEndTime())
                            && endTime.isAfter(booking.getStartTime());

                    return selectedConsole && overlaps;
                }))
                .toList();
    }
}
