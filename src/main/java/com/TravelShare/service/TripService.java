package com.TravelShare.service;

import com.TravelShare.dto.request.TripCreationRequest;
import com.TravelShare.dto.request.TripInvitationRequest;
import com.TravelShare.dto.request.TripUpdateRequest;
import com.TravelShare.dto.response.InvitationLinkResponse;
import com.TravelShare.dto.response.TripResponse;
import com.TravelShare.entity.*;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.mapper.TripMapper;
import com.TravelShare.repository.CurrencyRepository;
import com.TravelShare.repository.TripParticipantRepository;
import com.TravelShare.repository.TripRepository;
import com.TravelShare.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class TripService {
    final TripMapper tripMapper;
    final TripRepository tripRepository;
    final UserRepository userRepository;
    final CurrencyRepository currencyRepository;
    final TripParticipantRepository tripParticipantRepository;


    @Value("${app.invitation.base-url}")
    private String invitationBaseUrl;

    public TripResponse createTrip(TripCreationRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Currency currency = currencyRepository.findByCode(request.getDefaultCurrency())
                .orElseThrow(() -> new AppException(ErrorCode.CURRENCY_NOT_EXISTED));
        Trip trip = tripMapper.toTrip(request);
        trip.setCreatedBy(currentUser);
        trip.setCreatedAt(LocalDateTime.now());
        trip.setDefaultCurrency(currency);

        // Add the creator as a participant with ADMIN role
        trip.addParticipant(currentUser, "ADMIN", TripParticipant.InvitationStatus.ACTIVE);
        // Save trip to get ID generated
        trip = tripRepository.save(trip);
        // Add pending participants if specified
        if (request.getParticipants() != null) {
            for (TripInvitationRequest invitee : request.getParticipants()) {
                createInvitation(trip.getId(), invitee);
            }
        }
        return tripMapper.toTripResponse(trip);
    }

    public InvitationLinkResponse createInvitation(Long tripId, TripInvitationRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_EXISTED));

        // Generate unique token for this invitation
        String token = UUID.randomUUID().toString();

        TripParticipant participant = TripParticipant.builder()
                .trip(trip)
                .name(request.getName())
                .role(request.getRole() != null ? request.getRole() : "MEMBER")
                .invitationToken(token)
                .invitedAt(LocalDateTime.now())
                .status(TripParticipant.InvitationStatus.PENDING)
                .build();

        tripParticipantRepository.save(participant);

        String invitationLink = invitationBaseUrl + "/join/" + token;

        return InvitationLinkResponse.builder()
                .invitationToken(token)
                .invitationLink(invitationLink)
                .participantName(request.getName())
                .build();
    }

    public TripResponse acceptInvitation(String token) {
        TripParticipant participant = tripParticipantRepository.findByInvitationToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.INVITATION_NOT_FOUND));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Just update the participant without deleting it
        participant.setUser(currentUser);
        participant.setStatus(TripParticipant.InvitationStatus.ACTIVE);
        participant.setJoinedAt(LocalDateTime.now());

        participant = tripParticipantRepository.save(participant);

        return tripMapper.toTripResponse(participant.getTrip());
    }

    public List<InvitationLinkResponse> getTripInvitations(Long tripId) {
        List<TripParticipant> pendingParticipants = tripParticipantRepository
                .findByTripIdAndStatus(tripId, TripParticipant.InvitationStatus.PENDING);

        return pendingParticipants.stream()
                .map(p -> InvitationLinkResponse.builder()
                        .invitationToken(p.getInvitationToken())
                        .invitationLink(invitationBaseUrl + "/join/" + p.getInvitationToken())
                        .participantName(p.getName())
                        .build())
                .collect(Collectors.toList());
    }

    public TripResponse getTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_EXISTED));
        return tripMapper.toTripResponse(trip);
    }
    public TripResponse updateTrip(Long tripId, TripUpdateRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_EXISTED));
        tripMapper.updateTrip(trip, request);
        // Handle currency conversion manually if currency code is provided
        if (request.getDefaultCurrencyCode() != null) {
            Currency currency = currencyRepository.findByCode(request.getDefaultCurrencyCode())
                    .orElseThrow(() -> new AppException(ErrorCode.CURRENCY_NOT_EXISTED));
            trip.setDefaultCurrency(currency);
        }
        return tripMapper.toTripResponse(tripRepository.save(trip));
    }
    public void deleteTrip(Long tripId) {
        tripRepository.deleteById(tripId);
    }
    public TripResponse getTripById(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_EXISTED));
        return tripMapper.toTripResponse(trip);
    }
    public List<TripResponse> getAllTrips() {
        log.info("In method get Trips");
        return tripRepository.findAll().stream().map(tripMapper::toTripResponse).toList();
    }

    public void addParticipantToTrip(Long tripId, String username) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_EXISTED));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Check if current user has permission to add participants
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Tìm vai trò của người dùng hiện tại trong chuyến đi
        TripParticipant currentUserRole = tripParticipantRepository
                .findByTripIdAndUserId(tripId, currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        if (!"ADMIN".equals(currentUserRole.getRole())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        trip.addParticipant(user, "USER", TripParticipant.InvitationStatus.ACTIVE);
        tripRepository.save(trip);
    }

    public void removeParticipantFromTrip(Long tripId, String userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_EXISTED));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        // Check if current user has permission to remove participants
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        // Find the role of the current user in the trip
        TripParticipant currentUserRole = tripParticipantRepository
                .findByTripIdAndUserId(tripId, currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        if (!"ADMIN".equals(currentUserRole.getRole())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        trip.removeParticipant(user);
        tripRepository.save(trip);
    }
}
