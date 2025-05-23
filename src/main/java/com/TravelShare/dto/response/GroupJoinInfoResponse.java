package com.TravelShare.dto.response;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupJoinInfoResponse {
    private Long groupId;
    private String groupName;
    private List<GroupParticipantResponse> participants;
}