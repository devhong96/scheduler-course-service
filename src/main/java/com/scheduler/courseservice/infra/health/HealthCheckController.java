package com.scheduler.courseservice.infra.health;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("health-check")
@RequiredArgsConstructor
public class HealthCheckController {

    @Operation(
            summary = "네트워크 및 어플리케이션 상태 확인"
    )
    @GetMapping
    public ResponseEntity<Void> healthCheck() {
        return new ResponseEntity<>(OK);
    }
}
