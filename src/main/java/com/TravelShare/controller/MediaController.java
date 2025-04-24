package com.TravelShare.controller;

import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.MediaResponse;
import com.TravelShare.service.MediaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @PostMapping(value = "/trip/{tripId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResponse> uploadTripMedia(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long tripId,
            @RequestParam(value = "description", required = false) String description) {
        MediaResponse response = mediaService.uploadTripMedia(file, tripId, description);
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

    @GetMapping("/trip/{tripId}")
    public ApiResponse<List<MediaResponse>> getTripMedia(@PathVariable Long tripId) {
        return ApiResponse.<List<MediaResponse>>builder()
                .result(mediaService.getTripMedia(tripId))
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
