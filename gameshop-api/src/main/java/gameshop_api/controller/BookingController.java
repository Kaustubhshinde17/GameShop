
package gameshop_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.Valid;

import gameshop_api.dto.CreateBookingRequest;
import gameshop_api.model.Booking;
import gameshop_api.service.BookingService;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // Get all bookings
    @GetMapping
    public List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }

    // Create a new booking
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Booking createBooking(
            @Valid @RequestBody CreateBookingRequest request) {
        return bookingService.createBooking(request);
    }
@PutMapping("/{id}/confirm")
public Booking confirmBooking(@PathVariable int id) {
    return bookingService.confirmBooking(id);
}

@PutMapping("/{id}/cancel")
public Booking cancelBooking(@PathVariable int id) {
    return bookingService.cancelBooking(id);
}

}
