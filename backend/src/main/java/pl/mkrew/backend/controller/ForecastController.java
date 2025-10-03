package pl.mkrew.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.mkrew.backend.domain.entity.User;
import pl.mkrew.backend.dto.ForecastRequestDto;
import pl.mkrew.backend.dto.ForecastResponseDto;
import pl.mkrew.backend.repository.UserRepository;
import pl.mkrew.backend.service.ForecastService;

import java.util.List;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final ForecastService forecastService;
    private final UserRepository userRepository;

    public ForecastController(ForecastService forecastService, UserRepository userRepository) {
        this.forecastService = forecastService;
        this.userRepository = userRepository;
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ForecastResponseDto> createForecast(
            @RequestBody ForecastRequestDto requestDto,
            Authentication authentication) {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ForecastResponseDto response = forecastService.createForecastRequest(requestDto, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ForecastResponseDto> getForecast(@PathVariable Long id) {
        ForecastResponseDto response = forecastService.getForecastById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ForecastResponseDto>> getAllForecasts() {
        List<ForecastResponseDto> responses = forecastService.getAllForecasts();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/rckik/{rckikId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ForecastResponseDto>> getForecastsByRckik(@PathVariable Long rckikId) {
        List<ForecastResponseDto> responses = forecastService.getForecastsByRckik(rckikId);
        return ResponseEntity.ok(responses);
    }
}
