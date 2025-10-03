package pl.mkrew.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.mkrew.backend.domain.enums.BloodType;
import pl.mkrew.backend.dto.BloodInventoryResponse;
import pl.mkrew.backend.service.BloodInventoryService;

import java.util.List;

@RestController
@RequestMapping("/api/blood-inventory")
@PreAuthorize("hasAnyRole('USER_DATA', 'ADMIN')")
public class BloodInventoryController {

    private final BloodInventoryService bloodInventoryService;

    public BloodInventoryController(BloodInventoryService bloodInventoryService) {
        this.bloodInventoryService = bloodInventoryService;
    }

    @GetMapping("/current")
    public ResponseEntity<List<BloodInventoryResponse>> getAllCurrentInventory() {
        List<BloodInventoryResponse> inventory = bloodInventoryService.getAllCurrentInventory();
        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/current/{rckikCode}")
    public ResponseEntity<List<BloodInventoryResponse>> getCurrentInventoryByRCKiK(@PathVariable String rckikCode) {
        try {
            List<BloodInventoryResponse> inventory = bloodInventoryService.getCurrentInventoryByRCKiK(rckikCode);
            return ResponseEntity.ok(inventory);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/history/{rckikCode}")
    public ResponseEntity<List<BloodInventoryResponse>> getHistory(
            @PathVariable String rckikCode,
            @RequestParam BloodType bloodType,
            @RequestParam(defaultValue = "7") int period) {
        try {
            List<BloodInventoryResponse> history = bloodInventoryService.getHistory(rckikCode, bloodType, period);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
