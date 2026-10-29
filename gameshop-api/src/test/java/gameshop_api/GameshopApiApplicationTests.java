
package gameshop_api;

import gameshop_api.dto.CreateBookingRequest;
import gameshop_api.model.Booking;
import gameshop_api.model.BookingStatus;
import gameshop_api.model.GamingStation;
import gameshop_api.repository.BookingRepository;
import gameshop_api.repository.GamingStationRepository;
import gameshop_api.service.BookingService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
class GameshopApiApplicationTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldRejectBookingWithInvalidRequest() {
        BookingRepository bookingRepository =
                mock(BookingRepository.class);
        GamingStationRepository stationRepository =
                mock(GamingStationRepository.class);

        BookingService service =
                new BookingService(bookingRepository, stationRepository);

        CreateBookingRequest request = new CreateBookingRequest();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createBooking(request)
        );
    }

    @Test
    void shouldRejectZeroDuration() {
        BookingRepository bookingRepository =
                mock(BookingRepository.class);
        GamingStationRepository stationRepository =
                mock(GamingStationRepository.class);

        BookingService service =
                new BookingService(bookingRepository, stationRepository);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCustomerName("Test Customer");
        request.setStartTime(LocalDateTime.of(2026, 11, 10, 14, 0));
        request.setDurationHours(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createBooking(request)
        );
    }

    @Test
    void shouldRejectBookingBeforeOpeningTime() {
        BookingRepository bookingRepository =
                mock(BookingRepository.class);
        GamingStationRepository stationRepository =
                mock(GamingStationRepository.class);

        BookingService service =
                new BookingService(bookingRepository, stationRepository);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCustomerName("Test Customer");
        request.setStartTime(LocalDateTime.of(2026, 11, 10, 9, 0));
        request.setDurationHours(1);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createBooking(request)
        );
    }

    @Test
    void shouldRejectBookingBeyondClosingTime() {
        BookingRepository bookingRepository =
                mock(BookingRepository.class);
        GamingStationRepository stationRepository =
                mock(GamingStationRepository.class);

        BookingService service =
                new BookingService(bookingRepository, stationRepository);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCustomerName("Test Customer");
        request.setStartTime(LocalDateTime.of(2026, 11, 10, 22, 0));
        request.setDurationHours(2);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createBooking(request)
        );
    }

    @Test
    void shouldCreateValidPendingBooking() {
        BookingRepository bookingRepository =
                mock(BookingRepository.class);
        GamingStationRepository stationRepository =
                mock(GamingStationRepository.class);

        BookingService service =
                new BookingService(bookingRepository, stationRepository);

        LocalDateTime start = LocalDateTime.of(2026, 11, 10, 14, 0);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCustomerName("Test Customer");
        request.setStartTime(start);
        request.setDurationHours(2);

        List<GamingStation> stations = List.of(
                new GamingStation(
                        1, "PS5-01", new BigDecimal("150.00"), true)
        );

        when(stationRepository.findAll()).thenReturn(stations);
        when(stationRepository.findAllForUpdate()).thenReturn(stations);
        when(bookingRepository.findAll()).thenReturn(List.of());

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> {
                    Booking booking = invocation.getArgument(0);
                    booking.setBookingId(203);
                    return true;
                });

        Booking booking = service.createBooking(request);

        assertEquals(BookingStatus.PENDING, booking.getStatus());
        assertEquals(203, booking.getBookingId());
        assertEquals(start.plusHours(2), booking.getEndTime());
        assertEquals(new BigDecimal("150.00"), booking.getHourlyRate());
        assertEquals(new BigDecimal("300.00"), booking.getTotalCost());
    }

    @Test
    void shouldRejectBookingWhenAllConsolesAreOccupied() {
        BookingRepository bookingRepository =
                mock(BookingRepository.class);
        GamingStationRepository stationRepository =
                mock(GamingStationRepository.class);

        BookingService service =
                new BookingService(bookingRepository, stationRepository);

        LocalDateTime start = LocalDateTime.of(2026, 11, 10, 14, 0);
        LocalDateTime end = start.plusHours(2);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCustomerName("Test Customer");
        request.setStartTime(start);
        request.setDurationHours(2);

        List<GamingStation> stations = List.of(
                new GamingStation(
                        1, "PS5-01", new BigDecimal("150.00"), true),
                new GamingStation(
                        2, "PS5-02", new BigDecimal("150.00"), true)
        );

        when(stationRepository.findAll()).thenReturn(stations);
        when(stationRepository.findAllForUpdate()).thenReturn(stations);

        when(bookingRepository.findAll()).thenReturn(List.of(
                new Booking(
                        301, "Existing Customer 1", start, end, 2,
                        new BigDecimal("150.00"),
                        BookingStatus.CONFIRMED, 1),
                new Booking(
                        302, "Existing Customer 2", start, end, 2,
                        new BigDecimal("150.00"),
                        BookingStatus.CONFIRMED, 2)
        ));

        assertThrows(
                IllegalStateException.class,
                () -> service.createBooking(request)
        );

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void shouldCancelBookingSuccessfully() {
        BookingRepository bookingRepository =
                mock(BookingRepository.class);
        GamingStationRepository stationRepository =
                mock(GamingStationRepository.class);

        LocalDateTime start = LocalDateTime.of(2026, 11, 10, 14, 0);

        Booking existing = new Booking(
                401, "Test Customer", start, start.plusHours(2),
                2, new BigDecimal("150.00"),
                BookingStatus.CONFIRMED, 1);

        when(bookingRepository.findAll()).thenReturn(List.of(existing));
        when(bookingRepository.update(any(Booking.class))).thenReturn(true);

        BookingService service =
                new BookingService(bookingRepository, stationRepository);

        Booking result = service.cancelBooking(401);

        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        assertNull(result.getAssignedStationId());
    }

    @Test
    void shouldRejectCancellingAlreadyCancelledBooking() {
        BookingRepository bookingRepository =
                mock(BookingRepository.class);
        GamingStationRepository stationRepository =
                mock(GamingStationRepository.class);

        LocalDateTime start = LocalDateTime.of(2026, 11, 10, 14, 0);

        Booking existing = new Booking(
                402, "Test Customer", start, start.plusHours(2),
                2, new BigDecimal("150.00"),
                BookingStatus.CANCELLED, null);

        when(bookingRepository.findAll()).thenReturn(List.of(existing));

        BookingService service =
                new BookingService(bookingRepository, stationRepository);

        assertThrows(
                IllegalStateException.class,
                () -> service.cancelBooking(402)
        );
    }

    @Test
    void shouldRejectConfirmingNonexistentBooking() {
        BookingRepository bookingRepository =
                mock(BookingRepository.class);
        GamingStationRepository stationRepository =
                mock(GamingStationRepository.class);

        when(stationRepository.findAllForUpdate()).thenReturn(List.of());
        when(bookingRepository.findAll()).thenReturn(List.of());

        BookingService service =
                new BookingService(bookingRepository, stationRepository);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.confirmBooking(9999)
        );
    }

    @Test
    void shouldRejectConfirmingBookingThatIsNotPending() {
        BookingRepository bookingRepository =
                mock(BookingRepository.class);
        GamingStationRepository stationRepository =
                mock(GamingStationRepository.class);

        LocalDateTime start = LocalDateTime.of(2026, 11, 10, 14, 0);

        Booking existing = new Booking(
                403, "Test Customer", start, start.plusHours(2),
                2, new BigDecimal("150.00"),
                BookingStatus.CONFIRMED, 1);

        when(stationRepository.findAllForUpdate()).thenReturn(List.of());
        when(bookingRepository.findAll()).thenReturn(List.of(existing));

        BookingService service =
                new BookingService(bookingRepository, stationRepository);

        assertThrows(
                IllegalStateException.class,
                () -> service.confirmBooking(403)
        );
    }

    @Test
    void shouldConfirmPendingBookingAndAssignConsole() {
        BookingRepository bookingRepository =
                mock(BookingRepository.class);
        GamingStationRepository stationRepository =
                mock(GamingStationRepository.class);

        LocalDateTime start = LocalDateTime.of(2026, 11, 10, 14, 0);

        Booking pending = new Booking(
                404, "Test Customer", start, start.plusHours(2),
                2, new BigDecimal("150.00"),
                BookingStatus.PENDING, null);

        when(stationRepository.findAllForUpdate()).thenReturn(List.of(
                new GamingStation(
                        1, "PS5-01", new BigDecimal("150.00"), true)
        ));
        when(bookingRepository.findAll()).thenReturn(List.of(pending));
        when(bookingRepository.update(any(Booking.class))).thenReturn(true);

        BookingService service =
                new BookingService(bookingRepository, stationRepository);

        Booking result = service.confirmBooking(404);

        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        assertEquals(1, result.getAssignedStationId());
    }

    @Test
    void shouldRejectConfirmationWhenBothConsolesAreOccupied() {
        BookingRepository bookingRepository =
                mock(BookingRepository.class);
        GamingStationRepository stationRepository =
                mock(GamingStationRepository.class);

        LocalDateTime start = LocalDateTime.of(2026, 11, 10, 14, 0);
        LocalDateTime end = start.plusHours(2);

        Booking pending = new Booking(
                405, "New Customer", start, end, 2,
                new BigDecimal("150.00"), BookingStatus.PENDING, null);

        Booking occupied1 = new Booking(
                406, "Customer 1", start, end, 2,
                new BigDecimal("150.00"), BookingStatus.CONFIRMED, 1);

        Booking occupied2 = new Booking(
                407, "Customer 2", start, end, 2,
                new BigDecimal("150.00"), BookingStatus.CONFIRMED, 2);

        when(stationRepository.findAllForUpdate()).thenReturn(List.of(
                new GamingStation(
                        1, "PS5-01", new BigDecimal("150.00"), true),
                new GamingStation(
                        2, "PS5-02", new BigDecimal("150.00"), true)
        ));

        when(bookingRepository.findAll())
                .thenReturn(List.of(pending, occupied1, occupied2));

        BookingService service =
                new BookingService(bookingRepository, stationRepository);

        assertThrows(
                IllegalStateException.class,
                () -> service.confirmBooking(405)
        );
    }

    @Test
    void shouldAllowBookingWhenPreviousBookingEndsAtStartTime() {
        BookingRepository bookingRepository =
                mock(BookingRepository.class);
        GamingStationRepository stationRepository =
                mock(GamingStationRepository.class);

        LocalDateTime previousStart =
                LocalDateTime.of(2026, 11, 10, 14, 0);
        LocalDateTime newStart = previousStart.plusHours(2);

        Booking existing = new Booking(
                408, "Previous Customer", previousStart, newStart, 2,
                new BigDecimal("150.00"), BookingStatus.CONFIRMED, 1);

        List<GamingStation> stations = List.of(
                new GamingStation(
                        1, "PS5-01", new BigDecimal("150.00"), true)
        );

        when(stationRepository.findAll()).thenReturn(stations);
        when(stationRepository.findAllForUpdate()).thenReturn(stations);
        when(bookingRepository.findAll()).thenReturn(List.of(existing));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> {
                    Booking booking = invocation.getArgument(0);
                    booking.setBookingId(409);
                    return true;
                });

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCustomerName("Next Customer");
        request.setStartTime(newStart);
        request.setDurationHours(1);

        BookingService service =
                new BookingService(bookingRepository, stationRepository);

        Booking result = service.createBooking(request);

        assertEquals(BookingStatus.PENDING, result.getStatus());
        assertEquals(409, result.getBookingId());
    }

    @Test
    void shouldReturnStationsFromApi() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/stations")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                org.springframework.test.web.servlet.result
                        .MockMvcResultMatchers.status().isOk()
        );
    }
}
