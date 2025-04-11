package com.TravelShare.mapper;

import com.TravelShare.dto.response.MediaResponse;
import com.TravelShare.dto.response.UserResponse;
import com.TravelShare.entity.Media;
import com.TravelShare.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MediaMapper {
    @Mapping(source = "uploadedBy", target = "uploadedBy")
    MediaResponse toMediaResponse(Media media);
    // For converting string URL to Media entity (simple mapping)
    default Media toMedia(String mediaUrl) {
        if (mediaUrl == null) {
            return null;
        }
        return Media.builder()
                .fileUrl(mediaUrl)
                .build();
    }
}
