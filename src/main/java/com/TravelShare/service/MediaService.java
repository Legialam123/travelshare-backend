package com.TravelShare.service;

import com.TravelShare.dto.response.MediaResponse;
import com.TravelShare.entity.Expense;
import com.TravelShare.entity.Media;
import com.TravelShare.entity.Trip;
import com.TravelShare.entity.User;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.mapper.MediaMapper;
import com.TravelShare.repository.ExpenseRepository;
import com.TravelShare.repository.MediaRepository;
import com.TravelShare.repository.TripRepository;
import com.TravelShare.repository.UserRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.transform.Source;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class MediaService {
    final MediaRepository mediaRepository;
    final UserRepository userRepository;
    final MediaMapper mediaMapper;
    final TripRepository tripRepository;
    final ExpenseRepository expenseRepository;


    @Value("${app.file.storage-dir}")
    private String storageDir;

    @Value("${app.file.download-prefix}")
    private String urlPrefix;

    @Transactional
    public MediaResponse uploadMedia(MultipartFile file, String description) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

            Path uploadPath = Paths.get(storageDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = StringUtils.getFilenameExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + (fileExtension != null ? "." + fileExtension : "");

            //Store file
            Path filePath = uploadPath.resolve(uniqueFilename).normalize().toAbsolutePath();
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);


            // Determine media type from content type
            Media.MediaType mediaType = determineMediaType(file.getContentType());

            // Create and save media entity
            Media media = Media.builder()
                    .fileName(uniqueFilename)
                    .originalFileName(originalFilename)
                    .contentType(file.getContentType())
                    .mediaType(mediaType)
                    .fileSize(file.getSize())
                    .filePath(filePath.toString())
                    .fileUrl(urlPrefix + uniqueFilename)
                    .description(description)
                    .uploadedAt(LocalDateTime.now())
                    .uploadedBy(currentUser)
                    .build();
            media = mediaRepository.save(media);

            return mediaMapper.toMediaResponse(media);

        } catch (IOException e) {
            log.error("Failed to store file", e);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }
    @Transactional
    public MediaResponse uploadTripMedia (MultipartFile file, Long tripId, String description){
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            Trip trip = tripRepository.findById(tripId)
                    .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_EXISTED));
            Path uploadPath = Paths.get(storageDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = StringUtils.getFilenameExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + (fileExtension != null ? "." + fileExtension : "");

            //Store file
            Path filePath = uploadPath.resolve(uniqueFilename).normalize().toAbsolutePath();
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            Media.MediaType mediaType = determineMediaType(file.getContentType());
            // Create and save media entity
            Media media = Media.builder()
                    .fileName(uniqueFilename)
                    .originalFileName(originalFilename)
                    .contentType(file.getContentType())
                    .mediaType(mediaType)
                    .fileSize(file.getSize())
                    .trip(trip)
                    .filePath(filePath.toString())
                    .fileUrl(urlPrefix + uniqueFilename)
                    .description(description)
                    .uploadedAt(LocalDateTime.now())
                    .uploadedBy(currentUser)
                    .build();
            media = mediaRepository.save(media);

            return mediaMapper.toMediaResponse(media);
        }
        catch (IOException e) {
            log.error("Failed to store file", e);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Transactional
    public MediaResponse uploadExpenseMedia (MultipartFile file, Long expenseId, String description) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            Expense expense = expenseRepository.findById(expenseId)
                    .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_EXISTED));
            Path uploadPath = Paths.get(storageDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = StringUtils.getFilenameExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + (fileExtension != null ? "." + fileExtension : "");

            //Store file
            Path filePath = uploadPath.resolve(uniqueFilename).normalize().toAbsolutePath();
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            Media.MediaType mediaType = determineMediaType(file.getContentType());
            // Create and save media entity
            Media media = Media.builder()
                    .fileName(uniqueFilename)
                    .originalFileName(originalFilename)
                    .contentType(file.getContentType())
                    .mediaType(mediaType)
                    .fileSize(file.getSize())
                    .expense(expense)
                    .filePath(filePath.toString())
                    .fileUrl(urlPrefix + uniqueFilename)
                    .description(description)
                    .uploadedAt(LocalDateTime.now())
                    .uploadedBy(currentUser)
                    .build();
            media = mediaRepository.save(media);

            return mediaMapper.toMediaResponse(media);
        }
        catch (IOException e) {
            log.error("Failed to store file", e);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Transactional
    public MediaResponse uploadUserMedia (MultipartFile file, String description) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            Path uploadPath = Paths.get(storageDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = StringUtils.getFilenameExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + (fileExtension != null ? "." + fileExtension : "");

            //Store file
            Path filePath = uploadPath.resolve(uniqueFilename).normalize().toAbsolutePath();
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            Media.MediaType mediaType = determineMediaType(file.getContentType());
            // Create and save media entity
            Media media = Media.builder()
                    .fileName(uniqueFilename)
                    .originalFileName(originalFilename)
                    .contentType(file.getContentType())
                    .mediaType(mediaType)
                    .fileSize(file.getSize())
                    .user(currentUser)
                    .filePath(filePath.toString())
                    .fileUrl(urlPrefix + uniqueFilename)
                    .description(description)
                    .uploadedAt(LocalDateTime.now())
                    .uploadedBy(currentUser)
                    .build();
            media = mediaRepository.save(media);

            return mediaMapper.toMediaResponse(media);
        }
        catch (IOException e) {
            log.error("Failed to store file", e);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public MediaResponse getMedia (Long id){
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_EXISTED));
        return mediaMapper.toMediaResponse(media);
    }


    @Transactional(readOnly = true)
    public List<MediaResponse> getTripMedia (Long tripId){
        return mediaRepository.findByTripId(tripId).stream()
                .map(mediaMapper::toMediaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> getExpenseMedia ( Long expenseId){
        return mediaRepository.findByExpenseId(expenseId).stream()
                .map(mediaMapper::toMediaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> getUserMedia ( String userId){
        return mediaRepository.findByUserId(userId).stream()
                .map(mediaMapper::toMediaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> getUserUploads ( String userId){
        return mediaRepository.findByUserId(userId).stream()
                .map(mediaMapper::toMediaResponse)
                .toList();
    }

    @Transactional
    public void deleteMedia (Long id){
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_EXISTED));

        // Check if user has permission
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (media.getUploadedBy().getId().equals(currentUser.getId()) || currentUser.getRole().equals("ADMIN")) {
            // Delete physical file
            try {
                Path filePath = Paths.get(media.getFilePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.error("Failed to delete file", e);
                // Continue with database deletion even if file deletion fails
            }
        } else{
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }


        // Delete database record
        mediaRepository.delete(media);
    }

    private Media.MediaType determineMediaType (String contentType){
        if (contentType == null) {
            return Media.MediaType.OTHER;
        }

        if (contentType.startsWith("image/")) {
            return Media.MediaType.IMAGE;
        } else if (contentType.startsWith("video/")) {
            return Media.MediaType.VIDEO;
        } else if (contentType.startsWith("audio/")) {
            return Media.MediaType.AUDIO;
        } else if (contentType.equals("application/pdf") ||
                contentType.contains("document") ||
                contentType.contains("spreadsheet")) {
            return Media.MediaType.DOCUMENT;
        } else {
            return Media.MediaType.OTHER;
        }
    }

    public ResponseEntity<Resource> downloadFile (String fileName) throws IOException {
        Media media = mediaRepository.findByFileName(fileName)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_EXISTED));

        Path filePath = Paths.get(media.getFilePath());
        Resource resource= new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new AppException(ErrorCode.FILE_NOT_FOUND);
        }
        String contentType = media.getContentType();

        String name = media.getOriginalFileName() != null ?
                media.getOriginalFileName() : media.getFileName();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + name + "\"")
                .body(resource);
    }
}
