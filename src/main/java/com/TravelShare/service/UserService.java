package com.TravelShare.service;

import com.TravelShare.dto.request.UserCreationRequest;
import com.TravelShare.dto.request.UserUpdateRequest;
import com.TravelShare.dto.response.UserResponse;
import com.TravelShare.entity.Media;
import com.TravelShare.entity.User;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.mapper.UserMapper;
import com.TravelShare.repository.MediaRepository;
import com.TravelShare.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    EmailService emailService;
    MediaRepository mediaRepository;

    public UserResponse createUser(UserCreationRequest request) {
        //Check if user already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);

        }        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS);
        }

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setActive(false);
        if (user.getRole() == null) {
            user.setRole("USER");
        }
        if (user.getProfileImages() == null)
            user.setProfileImages(new HashSet<>());
        User savedUser = userRepository.save(user);

        //Send verification email
        emailService.sendVerificationEmail(savedUser);

        return userMapper.toUserResponse(savedUser);
    }

    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();
        User user = userRepository.findByUsername(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        UserResponse response = userMapper.toUserResponse(user);
        Optional<Media> avatarMedia = mediaRepository
                .findFirstByUserIdAndDescriptionOrderByUploadedAtDesc(user.getId(), "avatar");
        avatarMedia.ifPresent(media -> response.setAvatarUrl(media.getFileUrl()));

        return response;
    }

    public UserResponse updateUser(String userId,UserUpdateRequest  request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        userMapper.updateUser(user, request);
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            boolean isCorrect = passwordEncoder.matches(request.getOldPassword(), user.getPassword());
            if (isCorrect)
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            else {
                throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
            }
     }

        user.setUpdatedAt(LocalDateTime.now());
        return userMapper.toUserResponse(userRepository.save(user));
    }
    //@PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    //@PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        log.info("In method get Users");
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }

    //@PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUser(String id) {
        return userMapper.toUserResponse(
                userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }
}
