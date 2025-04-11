package com.TravelShare.repository;

import com.TravelShare.dto.response.MediaResponse;
import com.TravelShare.dto.response.UserResponse;
import com.TravelShare.entity.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media,Long>{
    List<Media> findByUploadedById(String userId);
    List<Media> findByTripId(Long tripId);
    List<Media> findByMediaType(Media.MediaType mediaType);
    List<Media> findByExpenseId(Long expenseId);
    List<Media> findByExpenseSplitId(Long expenseSplitId);
    List<Media> findByUserId(String userId);
    Optional<Media>  findByFileName(String fileName);
}
