package com.TravelShare.controller;

import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.MediaResponse;
import com.TravelShare.service.MediaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping({"/media"})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MediaController {
    MediaService mediaService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResponse> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        MediaResponse response = mediaService.uploadMedia(file, description);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/group/{groupId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResponse> uploadGroupMedia(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long groupId,
            @RequestParam(value = "description", required = false) String description) {
        MediaResponse response = mediaService.uploadGroupMedia(file, groupId, description);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/expense/{expenseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResponse> uploadExpenseMedia(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long expenseId,
            @RequestParam(value = "description", required = false) String description) {
        MediaResponse response = mediaService.uploadExpenseMedia(file, expenseId, description);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @PostMapping(value = "/user", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResponse> uploadUserMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        MediaResponse response = mediaService.uploadUserMedia(file, description);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @GetMapping("/{id}")
    public ResponseEntity<MediaResponse> getMedia(@PathVariable Long id) {
        return ResponseEntity.ok(mediaService.getMedia(id));
    }

    @GetMapping("/group/{groupId}")
    public ApiResponse<List<MediaResponse>> getGroupMedia(@PathVariable Long groupId) {
        return ApiResponse.<List<MediaResponse>>builder()
                .result(mediaService.getGroupMedia(groupId))
                .build();
    }

    @GetMapping("/expense/{expenseId}")
    public ApiResponse<List<MediaResponse>> getExpenseMedia(@PathVariable Long expenseId) {
        return ApiResponse.<List<MediaResponse>>builder()
                .result(mediaService.getExpenseMedia(expenseId))
                .build();
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<MediaResponse>> getUserMedia(@PathVariable String userId) {
        return ApiResponse.<List<MediaResponse>>builder()
                .result(mediaService.getUserMedia(userId))
                .build();
    }


    @GetMapping("/userupload/{userId}")
    public ApiResponse<List<MediaResponse>> getUploadUserMedia(@PathVariable String userId) {
        return ApiResponse.<List<MediaResponse>>builder()
                .result(mediaService.getUserUploads(userId))
                .build();
    }


    @DeleteMapping("/{id}")
    ApiResponse<String> deleteMedia(@PathVariable Long id) {
        mediaService.deleteMedia(id);
        return ApiResponse.<String>builder()
                .result("Media has been deleted")
                .build();
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadMedia(@PathVariable String fileName) throws IOException {
        return mediaService.downloadFile(fileName);
    }
}
