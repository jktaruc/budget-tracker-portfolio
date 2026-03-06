package com.budgettracker.backend.controller;

import com.budgettracker.backend.dto.AuthDTO;
import com.budgettracker.backend.service.AuthService;
import com.budgettracker.backend.service.DemoDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DemoDataService demoDataService;
    private final AuthService     authService;

    /** Resets the FREE demo account and returns a JWT. */
    @PostMapping("/reset")
    public ResponseEntity<AuthDTO.AuthResponse> resetFreeAndLogin() {
        String email = demoDataService.resetFreeDemo();
        AuthDTO.AuthResponse tokens = authService.login(
                new AuthDTO.LoginRequest(email, "demo1234")
        );
        return ResponseEntity.ok(tokens);
    }

    /** Resets the PRO demo account and returns a JWT. */
    @PostMapping("/reset-pro")
    public ResponseEntity<AuthDTO.AuthResponse> resetProAndLogin() {
        String email = demoDataService.resetProDemo();
        AuthDTO.AuthResponse tokens = authService.login(
                new AuthDTO.LoginRequest(email, "demo1234")
        );
        return ResponseEntity.ok(tokens);
    }
}
