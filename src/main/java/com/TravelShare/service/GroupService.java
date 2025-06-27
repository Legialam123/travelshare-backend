package com.TravelShare.service;

import com.TravelShare.dto.request.*;
import com.TravelShare.dto.response.*;
import com.TravelShare.entity.*;
import com.TravelShare.event.GroupUpdatedEvent;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.mapper.GroupMapper;
import com.TravelShare.repository.CategoryRepository;
import com.TravelShare.repository.CurrencyRepository;
import com.TravelShare.repository.GroupParticipantRepository;
import com.TravelShare.repository.GroupRepository;
import com.TravelShare.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class GroupService {
    final GroupMapper groupMapper;
    final GroupRepository groupRepository;
    final UserRepository userRepository;
    final CurrencyRepository currencyRepository;
    final GroupParticipantRepository groupParticipantRepository;
    final CategoryRepository categoryRepository;
    final ApplicationEventPublisher eventPublisher;
    final RequestService  requestService;
    final EmailService emailService;

    @Value("${app.invitation.base-url}")
    private String invitationBaseUrl;

    public GroupResponse createGroup(GroupCreationRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Currency currency = currencyRepository.findByCode(request.getDefaultCurrency())
                .orElseThrow(() -> new AppException(ErrorCode.CURRENCY_NOT_EXISTED));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));

        Group group = groupMapper.toGroup(request);
        
        group.setCategory(category);
        group.setCreatedBy(currentUser);
        group.setCreatedAt(LocalDateTime.now());
        group.setDefaultCurrency(currency);
        group.setJoinCode(generateJoinCode());

        //Tạo participant cho người tạo group (ADMIN)
        GroupParticipant adminParticipant = GroupParticipant.builder()
                .group(group)
                .user(currentUser)
                .name(request.getCreatorName() != null && !request.getCreatorName().isEmpty()
                        ? request.getCreatorName()
                        : currentUser.getFullName())
                .role("ADMIN")
                .status(GroupParticipant.InvitationStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();

        // Add vào cả 2 chiều (group <-> user)
        group.getParticipants().add(adminParticipant);
        currentUser.getGroups().add(adminParticipant);

        // Lưu group để tạo ID
        group = groupRepository.save(group);

        // Tạo thêm participants nếu có
        if (request.getParticipants() != null) {
            for (GroupInvitationRequest invitee : request.getParticipants()) {
                // Tạo participant trước
                GroupParticipant newParticipant = createParticipantForInvitation(group, invitee);
                // Thêm trực tiếp vào collection của group trong memory
                group.getParticipants().add(newParticipant);
            }
        }

        // Map và trả về ngay từ object trong memory
        return groupMapper.toGroupResponse(group);
    }

    private GroupParticipant createParticipantForInvitation(Group group, GroupInvitationRequest invitee) {
        GroupParticipant participant = GroupParticipant.builder()
                .group(group)
                .name(invitee.getName())
                .role("MEMBER")
                .build();

        if (invitee.getEmail() != null && !invitee.getEmail().isEmpty()) {
            User user = userRepository.findByEmail(invitee.getEmail()).orElse(null);
            if (user != null) {
                // Gán user luôn, ACTIVE
                participant.setUser(user);
                participant.setStatus(GroupParticipant.InvitationStatus.ACTIVE);
                participant.setJoinedAt(LocalDateTime.now());
            } else {
                // Chưa có tài khoản, gửi mời
                String token = UUID.randomUUID().toString();
                participant.setInvitationToken(token);
                participant.setInvitedAt(LocalDateTime.now());
                participant.setStatus(GroupParticipant.InvitationStatus.PENDING);

                // Gửi email mời đăng ký/tham gia nhóm
                String inviteLink = invitationBaseUrl + "/invite?token=" + token;
                emailService.sendInvitationEmail(invitee.getEmail(), group.getName(), inviteLink);
            }
        } else {
            // Không có email, chỉ tạo slot
            String token = UUID.randomUUID().toString();
            participant.setInvitationToken(token);
            participant.setInvitedAt(LocalDateTime.now());
            participant.setStatus(GroupParticipant.InvitationStatus.PENDING);
        }

        return groupParticipantRepository.save(participant);
    }

    private String generateJoinCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public GroupJoinInfoResponse getGroupJoinInfo(String joinCode) {
        Group group = groupRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new AppException(ErrorCode.JOIN_CODE_NOT_EXISTED));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        // Kiểm tra xem người dùng đã tham gia nhóm này chưa

        boolean alreadyInGroup = groupParticipantRepository
                .existsByGroupIdAndUserId(group.getId(), currentUser.getId());
        if (alreadyInGroup) { throw new AppException(ErrorCode.USER_ALREADY_IN_GROUP);
        }
        List<GroupParticipantResponse> participants = group.getParticipants().stream()
                .map(p -> GroupParticipantResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .user(p.getUser() != null ? UserSummaryResponse.from(p.getUser()) : null) // ✅ map đúng kiểu
                        .status(p.getStatus())
                        .role(p.getRole())
                        .joinedAt(p.getJoinedAt())
                        .build()
                )
                .collect(Collectors.toList());

        return GroupJoinInfoResponse.builder()
                .groupId(group.getId())
                .groupName(group.getName())
                .participants(participants)
                .build();
    }

    public ApiResponse<?> joinGroupByCode(JoinGroupRequest request) {
        Group group = groupRepository.findByJoinCode(request.getJoinCode())
                .orElseThrow(() -> new AppException(ErrorCode.JOIN_CODE_NOT_EXISTED));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        // Kiểm tra xem người dùng đã tham gia nhóm này chưa

        boolean alreadyInGroup = groupParticipantRepository
                .existsByGroupIdAndUserId(group.getId(), currentUser.getId());
        if (alreadyInGroup) {
        return ApiResponse.builder().message("Bạn đã tham gia nhóm này rồi").build();
        }

        // Trường hợp người dùng chọn một participant đã có sẵn (chưa liên kết)
        if (request.getParticipantId() != null) {
            GroupParticipant participant = groupParticipantRepository.findById(request.getParticipantId())
                    .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

            if (participant.getUser() != null) {
                throw new AppException(ErrorCode.PARTICIPANT_ALREADY_LINKED);
            }

            participant.setUser(currentUser);
            participant.setStatus(GroupParticipant.InvitationStatus.ACTIVE);
            groupParticipantRepository.save(participant);
            return ApiResponse.builder().message("Tham gia với tư cách thành viên có sẵn thành công").build();
        }

        // Trường hợp tạo participant mới
        // Kiểm tra nếu tên đã tồn tại trong cùng nhóm (tránh trùng)
        boolean nameExists = group.getParticipants().stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(request.getParticipantName()));
        if (nameExists) {
            return ApiResponse.builder().message("Tên đã tồn tại trong nhóm").build();
        }

        GroupParticipant newParticipant = new GroupParticipant();
        newParticipant.setGroup(group);
        newParticipant.setUser(currentUser);
        newParticipant.setName(request.getParticipantName() != null && !request.getParticipantName().isEmpty()
                ? request.getParticipantName()
                : currentUser.getFullName());
        newParticipant.setRole("MEMBER");
        newParticipant.setStatus(GroupParticipant.InvitationStatus.ACTIVE);

        groupParticipantRepository.save(newParticipant);

        return ApiResponse.builder().message("Đã tham gia nhóm thành công").build();
    }

    public void inviteParticipant(Long participantId, String email) {
        GroupParticipant participant = groupParticipantRepository.findById(participantId)
                .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

        if (participant.getUser() != null) {
            throw new AppException(ErrorCode.PARTICIPANT_ALREADY_LINKED);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXISTED));

        // Kiểm tra nếu user đã là thành viên nhóm
        boolean alreadyInGroup = participant.getGroup().getParticipants().stream()
                .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(user.getId()));
        if (alreadyInGroup) {
            throw new AppException(ErrorCode.USER_ALREADY_IN_GROUP);
        }

        // Lấy admin hiện tại (người thực hiện thao tác)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Tạo request JOIN_GROUP_INVITE (status PENDING)
        requestService.createRequest(
                RequestCreationRequest.builder()
                        .type("JOIN_GROUP_INVITE")
                        .receiverId(user.getId())
                        .groupId(participant.getGroup().getId())
                        .referenceId(participant.getId())
                        .build(),
                currentUser
        );
    }

    public InvitationLinkResponse createInvitation(Long groupId, GroupInvitationRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));

        // Generate unique token for this invitation
        String token = UUID.randomUUID().toString();

        GroupParticipant participant = GroupParticipant.builder()
                .group(group)
                .name(request.getName())
                .role(request.getRole() != null ? request.getRole() : "MEMBER")
                .invitationToken(token)
                .invitedAt(LocalDateTime.now())
                .status(GroupParticipant.InvitationStatus.PENDING)
                .build();

        groupParticipantRepository.save(participant);

        String invitationLink = invitationBaseUrl + "/join/" + token;

        return InvitationLinkResponse.builder()
                .invitationToken(token)
                .invitationLink(invitationLink)
                .participantName(request.getName())
                .build();
    }

    public GroupResponse acceptInvitation(String token) {
        GroupParticipant participant = groupParticipantRepository.findByInvitationToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.INVITATION_NOT_FOUND));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Just update the participant without deleting it
        participant.setUser(currentUser);
        participant.setStatus(GroupParticipant.InvitationStatus.ACTIVE);
        participant.setJoinedAt(LocalDateTime.now());

        participant = groupParticipantRepository.save(participant);

        return groupMapper.toGroupResponse(participant.getGroup());
    }

    public List<InvitationLinkResponse> getGroupInvitations(Long groupId) {
        List<GroupParticipant> pendingParticipants = groupParticipantRepository
                .findByGroupIdAndStatus(groupId, GroupParticipant.InvitationStatus.PENDING);

        return pendingParticipants.stream()
                .map(p -> InvitationLinkResponse.builder()
                        .invitationToken(p.getInvitationToken())
                        .invitationLink(invitationBaseUrl + "/join/" + p.getInvitationToken())
                        .participantName(p.getName())
                        .build())
                .collect(Collectors.toList());
    }

    public GroupResponse getGroup(Long groupId) {
        Group group = groupRepository.findByIdWithParticipants(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));
        return groupMapper.toGroupResponse(group);
    }

    public GroupResponse updateGroup(Long groupId, GroupUpdateRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));
        groupMapper.updateGroup(group, request);
        eventPublisher.publishEvent(new GroupUpdatedEvent(this, group, user));
        return groupMapper.toGroupResponse(group);
    }

    public void deleteGroup(Long groupId) {
        groupRepository.deleteById(groupId);
    }

    public List<GroupResponse> getAllGroups() {
        return groupRepository.findAll().stream()
                .map(groupMapper::toGroupResponse)
                .collect(Collectors.toList());
    }

    public List<GroupResponse> getMyGroups() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        List<Group> myGroups = groupRepository.findByParticipants_User_Id(currentUser.getId());
        return myGroups.stream()
                .map(groupMapper::toGroupResponse)
                .collect(Collectors.toList());
    }

    public ApiResponse<?> addParticipantToGroup(GroupParticipantCreationRequest request) {
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Người dùng cần phải là quản trị viên
        boolean isAdmin = group.getParticipants().stream()
                .anyMatch(p -> p.getUser() != null &&
                        p.getUser().getId().equals(currentUser.getId()) &&
                        "ADMIN".equals(p.getRole()));

        if (!isAdmin) {
            throw new AppException(ErrorCode.NOT_GROUP_ADMIN);
        }

        // Kiểm tra xem tên đã tồn tại chưa
        boolean nameExists = group.getParticipants().stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(request.getName()));

        if (nameExists) {
            return ApiResponse.builder()
                    .message("Tên đã tồn tại trong nhóm")
                    .build();
        }

        // Tạo participant mới
        GroupParticipant participant = GroupParticipant.builder()
                .group(group)
                .name(request.getName())
                .role(request.getRole())
                .status(GroupParticipant.InvitationStatus.PENDING)
                .build();

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);
            if (user != null) {
                // Kiểm tra xem người dùng đã tham gia nhóm chưa
                boolean alreadyInGroup = group.getParticipants().stream()
                        .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(user.getId()));

                if (alreadyInGroup) {
                    return ApiResponse.builder()
                            .message("Người dùng đã tham gia nhóm")
                            .build();
                }

                groupParticipantRepository.save(participant);

                // Tạo request JOIN_GROUP_INVITE
                requestService.createRequest(
                        RequestCreationRequest.builder()
                                .type("JOIN_GROUP_INVITE")
                                .receiverId(user.getId())
                                .groupId(group.getId())
                                .referenceId(participant.getId())
                                .build(),
                        currentUser
                );
                return ApiResponse.builder()
                        .message("Đã gửi lời mời tham gia nhóm, chờ xác nhận từ người dùng")
                        .build();
            } else {
                // Chưa có tài khoản
                String token = UUID.randomUUID().toString();
                participant.setInvitationToken(token);
                participant.setInvitedAt(LocalDateTime.now());
                groupParticipantRepository.save(participant);

                // Gửi email mời đăng ký/tham gia nhóm
                String inviteLink = invitationBaseUrl + "/invite?token=" + token;
                emailService.sendInvitationEmail(request.getEmail(), group.getName(), inviteLink);

                return ApiResponse.builder()
                        .message("Đã gửi email mời tham gia nhóm tới " + request.getEmail())
                        .build();
            }
        } else {
            // Không nhập email, chỉ tạo participant "slot"
            String token = UUID.randomUUID().toString();
            participant.setInvitationToken(token);
            participant.setInvitedAt(LocalDateTime.now());
            groupParticipantRepository.save(participant);

            // Có thể trả về link mời cho admin copy gửi cho ai đó
            String inviteLink = "https://your-app.com/invite?token=" + token;
            return ApiResponse.builder()
                    .message("Đã tạo slot thành viên, link mời: " + inviteLink)
                    .build();
        }
    }

    public void removeParticipantFromGroup(Long groupId, Long participantId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Kiểm tra xem người dùng hiện tại có quyền xóa hay không (phải là admin)
        boolean isAdmin = group.getParticipants().stream()
                .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(currentUser.getId()) && "ADMIN".equals(p.getRole()));

        if (!isAdmin) {
            throw new AppException(ErrorCode.NOT_GROUP_ADMIN);
        }

        GroupParticipant participant = groupParticipantRepository.findById(participantId)
                .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

        participant.setUser(null);
        participant.setStatus(GroupParticipant.InvitationStatus.PENDING);
        participant.setInvitationToken(null);
        participant.setJoinedAt(null);
        participant.setInvitedAt(null);
        groupParticipantRepository.save(participant);
    }

    public void leaveGroup(Long groupId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        GroupParticipant participant = groupParticipantRepository.findByGroupIdAndUserId(groupId, currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_GROUP_MEMBER));

        // Kiểm tra nếu là admin cuối cùng
        if ("ADMIN".equals(participant.getRole()) && 
                groupParticipantRepository.countByGroupIdAndRole(groupId, "ADMIN") <= 1) {
            throw new AppException(ErrorCode.CANNOT_LEAVE_AS_LAST_ADMIN);
        }

        participant.setUser(null);
        participant.setStatus(GroupParticipant.InvitationStatus.PENDING);
        participant.setInvitationToken(null);
        participant.setJoinedAt(null);
        participant.setInvitedAt(null);
        groupParticipantRepository.save(participant);
    }

    public void updateParticipantRole(UpdateParticipantRole request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));

        // Kiểm tra xem người dùng hiện tại có quyền thay đổi vai trò hay không (phải là admin)
        boolean isAdmin = group.getParticipants().stream()
                .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(currentUser.getId()) && "ADMIN".equals(p.getRole()));

        if (!isAdmin) {
            throw new AppException(ErrorCode.NOT_GROUP_ADMIN);
        }

        GroupParticipant participant = groupParticipantRepository.findById(request.getParticipantId())
                .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

        // Nếu hạ vai trò của admin, phải đảm bảo không phải admin cuối cùng
        if ("ADMIN".equals(participant.getRole()) && !"ADMIN".equals(request.getNewRole()) && 
                groupParticipantRepository.countByGroupIdAndRole(group.getId(), "ADMIN") <= 1) {
            throw new AppException(ErrorCode.CANNOT_DEMOTE_LAST_ADMIN);
        }

        participant.setRole(request.getNewRole());
        groupParticipantRepository.save(participant);
    }

    public void updateParticipantName(UpdateParticipantName request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));

        // Chỉ cho phép thành viên tự đổi tên của mình
        GroupParticipant participant = groupParticipantRepository.findById(request.getParticipantId())
                .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

        if (participant.getUser() == null || !participant.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.NOT_GROUP_MEMBER);
        }
        if (!participant.getGroup().getId().equals(group.getId())) {
            throw new AppException(ErrorCode.PARTICIPANT_NOT_IN_GROUP);
        }

        participant.setName(request.getNewName());
        groupParticipantRepository.save(participant);
    }
} 