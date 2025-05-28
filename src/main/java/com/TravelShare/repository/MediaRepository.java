package com.TravelShare.repository;

import com.TravelShare.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media,Long>{
    List<Media> findByUploadedById(String userId);
    List<Media> findByGroupId(Long groupId);
    List<Media> findByMediaType(Media.MediaType mediaType);
    List<Media> findByExpenseId(Long expenseId);
    List<Media> findByUserIdOrderByUploadedAtDesc(String userId);
    Optional<Media>  findByFileName(String fileName);
    Optional<Media> findFirstByGroupIdAndMediaTypeAndDescription(Long groupId, Media.MediaType mediaType, String description);
    Optional<Media> findFirstByUserIdAndDescriptionOrderByUploadedAtDesc(String userId, String description);
}
