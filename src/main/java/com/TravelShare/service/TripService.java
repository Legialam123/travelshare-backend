package com.TravelShare.service;

import com.TravelShare.dto.request.*;
import com.TravelShare.dto.response.*;
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
import org.hibernate.sql.Update;
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
        trip.setJoinCode(generateJoinCode());

        //Tạo participant cho người tạo trip (ADMIN)
        TripParticipant adminParticipant = TripParticipant.builder()
                .trip(trip)
                .user(currentUser)
                .name(currentUser.getFullName())
                .role("ADMIN")
                .status(TripParticipant.InvitationStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();

        // Add vào cả 2 chiều (trip <-> user)
        trip.getParticipants().add(adminParticipant);
        currentUser.getTrips().add(adminParticipant);

        // Lưu trip để tạo ID
        trip = tripRepository.save(trip);

        // Tạo thêm lời mời nếu có
        if (request.getParticipants() != null) {
            for (TripInvitationRequest invitee : request.getParticipants()) {
                createInvitation(trip.getId(), invitee);
            }
        }

        return tripMapper.toTripResponse(trip);
    }


    private String generateJoinCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public TripJoinInfoResponse getTripJoinInfo(String joinCode) {
        Trip trip = tripRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_EXISTED));

        List<TripParticipantResponse> participants = trip.getParticipants().stream()
                .map(p -> TripParticipantResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .user(p.getUser() != null ? UserSummaryResponse.from(p.getUser()) : null) // ✅ map đúng kiểu
                        .status(p.getStatus())
                        .role(p.getRole())
                        .joinedAt(p.getJoinedAt())
                        .build()
                )
                .collect(Collectors.toList());

        return TripJoinInfoResponse.builder()
                .tripId(trip.getId())
                .tripName(trip.getName())
                .participants(participants)
                .build();
    }



    public ApiResponse<?> joinTripByCode(JoinTripRequest request) {
        Trip trip = tripRepository.findByJoinCode(request.getJoinCode())
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_EXISTED));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Trường hợp người dùng chọn một participant đã có sẵn (chưa liên kết)
        if (request.getParticipantId() != null) {
            TripParticipant participant = tripParticipantRepository.findById(request.getParticipantId())
                    .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

            if (participant.getUser() != null) {
                throw new AppException(ErrorCode.PARTICIPANT_ALREADY_LINKED);
            }

            participant.setUser(currentUser);
            participant.setStatus(TripParticipant.InvitationStatus.ACTIVE);
            tripParticipantRepository.save(participant);
            return ApiResponse.builder().message("Tham gia với tư cách thành viên có sẵn thành công").build();
        }

        // Trường hợp tạo participant mới
        // Kiểm tra nếu tên đã tồn tại trong cùng chuyến đi (tránh trùng)
        boolean nameExists = trip.getParticipants().stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(request.getParticipantName()));
        if (nameExists) {
            return ApiResponse.builder().message("Tên đã tồn tại trong chuyến đi").build();
        }

        TripParticipant newParticipant = new TripParticipant();
        newParticipant.setTrip(trip);
        newParticipant.setUser(currentUser);
        newParticipant.setName(request.getParticipantName() != null && !request.getParticipantName().isEmpty()
                ? request.getParticipantName()
                : currentUser.getFullName());
        newParticipant.setRole("MEMBER");
        newParticipant.setStatus(TripParticipant.InvitationStatus.ACTIVE);

        tripParticipantRepository.save(newParticipant);

        return ApiResponse.builder().message("Đã tham gia chuyến đi thành công").build();
    }

    public void inviteParticipant(Long participantId, String email) {
        TripParticipant participant = tripParticipantRepository.findById(participantId)
                .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

        if (participant.getUser() != null) {
            throw new AppException(ErrorCode.PARTICIPANT_ALREADY_LINKED);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXISTED));

        participant.setUser(user);
        participant.setStatus(TripParticipant.InvitationStatus.ACTIVE);
        participant.setJoinedAt(LocalDateTime.now());

        tripParticipantRepository.save(participant);
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

    public List<TripResponse> getMyTrips() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<Trip> trips = tripRepository.findByParticipants_User_Id(currentUser.getId());
        return trips.stream()
                .map(tripMapper::toTripResponse)
                .collect(Collectors.toList());
    }


    public ApiResponse<?> addParticipantToTrip(TripParticipantCreationRequest request) {
        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_EXISTED));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TripParticipant currentUserParticipant = tripParticipantRepository
                .findByTripIdAndUserId(request.getTripId(), currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        if (!"ADMIN".equals(currentUserParticipant.getRole())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Nếu người dùng có nhập email → tìm user và tạo participant đã liên kết
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

            boolean alreadyInTrip = tripParticipantRepository
                    .existsByTripIdAndUserId(request.getTripId(), user.getId());
            if (alreadyInTrip) {
                throw new AppException(ErrorCode.USER_ALREADY_IN_TRIP);
            }

            TripParticipant participant = TripParticipant.builder()
                    .trip(trip)
                    .user(user)
                    .name(user.getFullName())
                    .role(request.getRole() != null ? request.getRole() : "MEMBER")
                    .status(TripParticipant.InvitationStatus.ACTIVE)
                    .joinedAt(LocalDateTime.now())
                    .build();

            tripParticipantRepository.save(participant);

            return ApiResponse.builder()
                    .message("Thêm thành viên đã liên kết thành công")
                    .build();
        }

        // Nếu không có email → tạo participant chờ duyệt với token
        TripInvitationRequest inviteRequest = TripInvitationRequest.builder()
                .name(request.getName())
                .role(request.getRole())
                .build();

        InvitationLinkResponse invitation = createInvitation(request.getTripId(), inviteRequest);

        return ApiResponse.builder()
                .message("Thêm người tham gia và lời mời tham gia Trip thành công")
                .result(invitation)
                .build();
    }



    public void removeParticipantFromTrip(Long tripId, Long participantId) {
        TripParticipant participant = tripParticipantRepository.findById(participantId)
                .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TripParticipant currentUserParticipant = tripParticipantRepository
                .findByTripIdAndUserId(tripId, currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        if (!"ADMIN".equals(currentUserParticipant.getRole())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        tripParticipantRepository.delete(participant);
    }

    public void leaveTrip(Long tripId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TripParticipant participant = tripParticipantRepository
                .findByTripIdAndUserId(tripId, currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

        if ("ADMIN".equals(participant.getRole())) {
            long adminCount = tripParticipantRepository.countByTripIdAndRole(tripId, "ADMIN");
            if (adminCount <= 1) {
                throw new AppException(ErrorCode.CANNOT_LEAVE_TRIP_AS_LAST_ADMIN);
            }
        }

        tripParticipantRepository.delete(participant);
    }

    public void updateParticipantRole(UpdateParticipantRole request) {
        TripParticipant participant = tripParticipantRepository.findById(request.getParticipantId())
                .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TripParticipant currentUserParticipant = tripParticipantRepository
                .findByTripIdAndUserId(request.getTripId(), currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        if (!"ADMIN".equals(currentUserParticipant.getRole())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if ("ADMIN".equals(participant.getRole()) && !"ADMIN".equals(request.getNewRole())) {
            long adminCount = tripParticipantRepository.countByTripIdAndRole(request.getTripId(), "ADMIN");
            if (adminCount <= 1) {
                throw new AppException(ErrorCode.CANNOT_REMOVE_LAST_ADMIN);
            }
        }

        participant.setRole(request.getNewRole());
        tripParticipantRepository.save(participant);
    }

}
