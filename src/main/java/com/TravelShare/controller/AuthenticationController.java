package com.TravelShare.controller;

import com.TravelShare.dto.request.*;
import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.AuthenticationResponse;
import com.TravelShare.dto.response.IntrospectResponse;
import com.TravelShare.repository.UserRepository;
import com.TravelShare.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class    AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/login")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request,
                                                     HttpServletRequest httpRequest) {
        var result = authenticationService.authenticate(request, httpRequest);
        return ApiResponse.<AuthenticationResponse>builder().result(result).build();
    }

    @PostMapping("/introspect")
    ApiResponse<IntrospectResponse> authenticate(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder().result(result).build();
    }

    @PostMapping("/refresh")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody RefreshRequest request,
                                                     HttpServletRequest httpRequest)
            throws ParseException, JOSEException {
        var result = authenticationService.refreshToken(request, httpRequest);
        return ApiResponse.<AuthenticationResponse>builder().result(result).build();
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestBody LogoutRequest request) throws ParseException, JOSEException {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder().build();
    }
}