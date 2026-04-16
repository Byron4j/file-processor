package com.fileprocessor.controller;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.dto.LoginRequest;
import com.fileprocessor.dto.LoginResponse;
import com.fileprocessor.dto.RefreshTokenRequest;
import com.fileprocessor.security.CustomUserDetailsService;
import com.fileprocessor.security.JwtTokenProvider;
import com.fileprocessor.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @PostMapping("/login")
    public ResponseEntity<FileResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);

        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(tokenProvider.getAccessTokenExpiration() / 1000);
        response.setTokenType("Bearer");
        response.setUsername(userPrincipal.getUsername());
        response.setRoles(roles);

        return ResponseEntity.ok(FileResponse.builder()
                .success(true)
                .message("Login successful")
                .data(response)
                .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<FileResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        if (!tokenProvider.validateToken(request.getRefreshToken())) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("Invalid refresh token")
                            .build()
            );
        }

        String username = tokenProvider.getUsernameFromToken(request.getRefreshToken());
        UserPrincipal userPrincipal = (UserPrincipal) userDetailsService.loadUserByUsername(username);

        String newAccessToken = tokenProvider.generateAccessToken(userPrincipal);

        LoginResponse refreshResponse = new LoginResponse();
        refreshResponse.setAccessToken(newAccessToken);
        refreshResponse.setExpiresIn(tokenProvider.getAccessTokenExpiration() / 1000);
        refreshResponse.setTokenType("Bearer");
        refreshResponse.setUsername(userPrincipal.getUsername());

        return ResponseEntity.ok(FileResponse.builder()
                .success(true)
                .message("Token refreshed")
                .data(refreshResponse)
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<FileResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("Not authenticated")
                            .build()
            );
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());

        return ResponseEntity.ok(FileResponse.builder()
                .success(true)
                .message("User info retrieved")
                .data(java.util.Map.of(
                        "username", userPrincipal.getUsername(),
                        "roles", roles
                ))
                .build());
    }
}
