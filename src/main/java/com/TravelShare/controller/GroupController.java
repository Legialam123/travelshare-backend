package com.TravelShare.controller;

import com.TravelShare.dto.request.*;
import com.TravelShare.dto.response.*;
import com.TravelShare.service.GroupService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class GroupController {
    GroupService groupService;

    @PutMapping("/{groupId}/participant/{participantId}/update_role")
    public ApiResponse<String> updateParticipantRole(@RequestBody UpdateParticipantRole request) {
        groupService.updateParticipantRole(request);
        return ApiResponse.<String>builder()
                .result("Vai trò đã được cập nhật thành công")
                .build();
    }

    @PutMapping("/{groupId}/participant/{participantId}/update_name")
    public ApiResponse<String> updateParticipantName(@RequestBody UpdateParticipantName request) {
        groupService.updateParticipantName(request);
        return ApiResponse.<String>builder()
                .result("Tên đã được cập nhật thành công")
                .build();
    }

    @DeleteMapping("/{groupId}/leave")
    public ApiResponse<String> leaveGroup(@PathVariable Long groupId) {
        groupService.leaveGroup(groupId);
        return ApiResponse.<String>builder()
                .result("You have left the group")
                .build();
    }

    @DeleteMapping("/{groupId}/participant/{participantId}")
    public ApiResponse<String> removeParticipantsFromGroup(
            @PathVariable Long groupId,
            @PathVariable Long participantId) {
        groupService.removeParticipantFromGroup(groupId, participantId);
        return ApiResponse.<String>builder()
                .result("Participant has been removed from the group")
                .build();
    }

    @PostMapping("/{groupId}/participant")
    public ApiResponse<?> addParticipantToGroup(
            @PathVariable Long groupId,
            @RequestBody GroupParticipantCreationRequest request) {

        request.setGroupId(groupId); // Gán groupId từ path vào request DTO
        return groupService.addParticipantToGroup(request);
    }


    @PostMapping("/join")
    public ApiResponse<?> joinGroupByCode(@RequestBody JoinGroupRequest request) {
        return groupService.joinGroupByCode(request);
    }

    @GetMapping("/join-info/{joinCode}")
    public ApiResponse<GroupJoinInfoResponse> getGroupJoinInfo(@PathVariable String joinCode) {
        return ApiResponse.<GroupJoinInfoResponse>builder()
                .result(groupService.getGroupJoinInfo(joinCode))
                .build();
    }

    @PostMapping("/invite")
    public ApiResponse<?> inviteParticipant(@RequestBody InviteParticipantRequest request) {
        return groupService.inviteParticipant(request.getParticipantId(), request.getEmail());
    }

    @GetMapping
    ApiResponse<List<GroupResponse>> getAllGroups() {
        return ApiResponse.<List<GroupResponse>>builder()
                .result(groupService.getAllGroups())
                .build();
    }

    @GetMapping("/myGroups")
    ApiResponse<List<GroupResponse>> getMyGroups() {
        return ApiResponse.<List<GroupResponse>>builder()
                .result(groupService.getMyGroups())
                .build();
    }

    @GetMapping("/{groupId}")
    ApiResponse<GroupResponse> getGroup(@PathVariable Long groupId) {
        return ApiResponse.<GroupResponse>builder()
                .result(groupService.getGroup(groupId))
                .build();
    }

    @PostMapping
    ApiResponse<GroupResponse> createGroup(@RequestBody GroupCreationRequest request) {
        return ApiResponse.<GroupResponse>builder()
                .result(groupService.createGroup(request))
                .build();
    }

    @PutMapping("/{groupId}")
    ApiResponse<GroupResponse> updateGroup(@PathVariable Long groupId, @RequestBody GroupUpdateRequest request) {
        return ApiResponse.<GroupResponse>builder()
                .result(groupService.updateGroup(groupId, request))
                .build();
    }

    @DeleteMapping("/{groupId}")
    ApiResponse<String> deleteGroup(@PathVariable Long groupId) {
        groupService.deleteGroup(groupId);
        return ApiResponse.<String>builder()
                .result("Group has been deleted")
                .build();
    }
}
