package com.TravelShare.controller;

import com.TravelShare.dto.request.*;
import com.TravelShare.dto.response.*;
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

    @PutMapping("/{tripId}/participant/{participantId}/update_role")
    public ApiResponse<String> updateParticipantRole(@RequestBody UpdateParticipantRole request) {
        tripService.updateParticipantRole(request);
        return ApiResponse.<String>builder()
                .result("Vai trò đã được cập nhật thành công")
                .build();
    }

    @DeleteMapping("/{tripId}/leave")
    public ApiResponse<String> leaveTrip(@PathVariable Long tripId) {
        tripService.leaveTrip(tripId);
        return ApiResponse.<String>builder()
                .result("You have left the trip")
                .build();
    }

    @DeleteMapping("/{tripId}/participant/{participantId}")
    public ApiResponse<String> removeParticipantsFromTrip(
            @PathVariable Long tripId,
            @PathVariable Long participantId) {
        tripService.removeParticipantFromTrip(tripId, participantId);
        return ApiResponse.<String>builder()
                .result("Participant has been removed from trip")
                .build();
    }

    @PostMapping("/{tripId}/participant")
    public ApiResponse<?> addParticipantToTrip(
            @PathVariable Long tripId,
            @RequestBody TripParticipantCreationRequest request) {

        request.setTripId(tripId); // Gán tripId từ path vào request DTO
        return tripService.addParticipantToTrip(request);
    }


    @PostMapping("/join")
    public ApiResponse<?> joinTripByCode(@RequestBody JoinTripRequest request) {
        return tripService.joinTripByCode(request);
    }

    @GetMapping("/join-info/{joinCode}")
    public ApiResponse<TripJoinInfoResponse> getTripJoinInfo(@PathVariable String joinCode) {
        return ApiResponse.<TripJoinInfoResponse>builder()
                .result(tripService.getTripJoinInfo(joinCode))
                .build();
    }

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

    @PostMapping("/invite")
    public ApiResponse<?> inviteParticipant(@RequestBody InviteParticipantRequest request) {
        tripService.inviteParticipant(request.getParticipantId(), request.getEmail());
        return ApiResponse.builder()
                .message("✅ Đã mời thành viên thành công")
                .build();
    }

    @GetMapping
    ApiResponse<List<TripResponse>> getAllTrips() {
        return ApiResponse.<List<TripResponse>>builder()
                .result(tripService.getAllTrips())
                .build();
    }

    @GetMapping("/myTrips")
    ApiResponse<List<TripResponse>> getMyTrips() {
        return ApiResponse.<List<TripResponse>>builder()
                .result(tripService.getMyTrips())
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
