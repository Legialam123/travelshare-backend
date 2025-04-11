package com.TravelShare.controller;

import com.TravelShare.dto.request.TripCreationRequest;
import com.TravelShare.dto.request.TripInvitationRequest;
import com.TravelShare.dto.request.TripUpdateRequest;
import com.TravelShare.dto.request.UserCreationRequest;
import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.InvitationLinkResponse;
import com.TravelShare.dto.response.TripResponse;
import com.TravelShare.dto.response.UserResponse;
import com.TravelShare.service.TripService;
import com.TravelShare.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trip")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TripController {
    TripService tripService;

    @PostMapping("/{tripId}/invite")
    public ApiResponse<InvitationLinkResponse> inviteToTrip(
            @PathVariable Long tripId,
            @RequestBody @Valid TripInvitationRequest request) {
        return ApiResponse.<InvitationLinkResponse>builder()
                .result(tripService.createInvitation(tripId, request))
                .build();
    }

    @GetMapping("/{tripId}/invitations")
    public ApiResponse<List<InvitationLinkResponse>> getTripInvitations(@PathVariable Long tripId) {
        return ApiResponse.<List<InvitationLinkResponse>>builder()
                .result(tripService.getTripInvitations(tripId))
                .build();
    }

    @PostMapping("/invite/accept/{token}")
    public ApiResponse<TripResponse> acceptInvitation(@PathVariable String token) {
        return ApiResponse.<TripResponse>builder()
                .result(tripService.acceptInvitation(token))
                .build();
    }

    @GetMapping
    ApiResponse<List<TripResponse>> getAllTrips() {
        return ApiResponse.<List<TripResponse>>builder()
                .result(tripService.getAllTrips())
                .build();
    }

    @GetMapping("/{tripId}")
    ApiResponse<TripResponse> getTrip(@PathVariable Long tripId) {
        return ApiResponse.<TripResponse>builder()
                .result(tripService.getTrip(tripId))
                .build();
    }

    @PostMapping
    ApiResponse<TripResponse> createTrip(@RequestBody TripCreationRequest request) {
        return ApiResponse.<TripResponse>builder()
                .result(tripService.createTrip(request))
                .build();
    }

    @PutMapping("/{tripId}")
    ApiResponse<TripResponse> updateTrip(@PathVariable Long tripId, @RequestBody TripUpdateRequest request) {
        return ApiResponse.<TripResponse>builder()
                .result(tripService.updateTrip(tripId, request))
                .build();
    }

    @DeleteMapping("/{tripId}")
    ApiResponse<String> deleteTrip(@PathVariable Long tripId) {
        tripService.deleteTrip(tripId);
        return ApiResponse.<String>builder()
                .result("Trip has been deleted")
                .build();
    }
}
