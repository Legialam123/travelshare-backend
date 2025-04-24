package com.TravelShare.dto.response;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripJoinInfoResponse {
    private Long tripId;
    private String tripName;
    private List<TripParticipantResponse> participants;
}