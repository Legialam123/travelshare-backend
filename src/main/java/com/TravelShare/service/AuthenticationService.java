package com.TravelShare.service;

import com.TravelShare.dto.request.*;
import com.TravelShare.dto.response.AuthenticationResponse;
import com.TravelShare.dto.response.IntrospectResponse;
import com.TravelShare.entity.InvalidatedToken;
import com.TravelShare.entity.RefreshToken;
import com.TravelShare.entity.User;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.repository.InvalidatedTokenRepository;
import com.TravelShare.repository.RefreshTokenRepository;
import com.TravelShare.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;
    RefreshTokenRepository refreshTokenRepository;
    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    /*
    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;

        try {
            verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }*/

    public AuthenticationResponse authenticate(AuthenticationRequest request, HttpServletRequest httpRequest) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        var user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (!user.isActive()) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

        String fingerprint = generateFingerprint(httpRequest);
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user, fingerprint);

        return AuthenticationResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    // Logout - invalidate both tokens
    public void logout(LogoutRequest request) {
        try {
            SignedJWT accessToken = SignedJWT.parse(request.getToken());
            SignedJWT refreshToken = SignedJWT.parse(request.getRefreshToken());

            // Invalidate access token
            String accessJwtId = accessToken.getJWTClaimsSet().getJWTID();
            Date accessExpiryTime = accessToken.getJWTClaimsSet().getExpirationTime();

            invalidatedTokenRepository.save(InvalidatedToken.builder()
                    .id(accessJwtId)
                    .expiryTime(accessExpiryTime)
                    .build());

            // Revoke refresh token
            String refreshJwtId = refreshToken.getJWTClaimsSet().getJWTID();
            refreshTokenRepository.findById(refreshJwtId).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });

        } catch (Exception e) {
            log.info("Error during logout: {}", e.getMessage());
        }
    }



    // Refresh tokens using refresh token
    public AuthenticationResponse refreshToken(RefreshRequest request, HttpServletRequest httpRequest) throws JOSEException, ParseException {
        SignedJWT signedJWT = verifyToken(request.getRefreshToken());

        if (!"refresh".equals(signedJWT.getJWTClaimsSet().getStringClaim("tokenType"))) {
            throw new AppException(ErrorCode.INVALID_TOKEN_TYPE);
        }

        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        String username = signedJWT.getJWTClaimsSet().getSubject();

        // Get refresh token from database
        RefreshToken refreshToken = refreshTokenRepository.findByIdAndRevoked(jwtId, false)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        // Verify fingerprint
        String currentFingerprint = generateFingerprint(httpRequest);
        if (!refreshToken.getFingerprint().equals(currentFingerprint)) {
            // Potential token theft - revoke token and throw exception
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new AppException(ErrorCode.UNAUTHORIZED_DEVICE);
        }

        // Implement token rotation - revoke current refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Generate new tokens
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String newAccessToken = generateAccessToken(user);
        String newRefreshToken = generateRefreshToken(user, currentFingerprint);

        return AuthenticationResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .authenticated(true)
                .build();
    }

    private String generateAccessToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("yalam.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("role",user.getRole())
                .claim("tokenType", "access")
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    // Generate refresh token with fingerprint
    private String generateRefreshToken(User user, String fingerprint) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        String jwtId = UUID.randomUUID().toString();
        Date expiryTime = new Date(
                Instant.now().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli());

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("travelshare.com")
                .issueTime(new Date())
                .expirationTime(expiryTime)
                .jwtID(jwtId)
                .claim("tokenType", "refresh")
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));

            // Save refresh token in database for revocation control
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(jwtId)
                    .username(user.getUsername())
                    .expiryTime(expiryTime)
                    .revoked(false)
                    .fingerprint(fingerprint)
                    .build();

            refreshTokenRepository.save(refreshToken);

            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create refresh token", e);
            throw new RuntimeException(e);
        }
    }

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;

        try {
            // Your verifyToken method takes only one parameter, not two
            verifyToken(token);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

        // Generate device fingerprint from request
    private String generateFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent") != null ?
                request.getHeader("User-Agent") : "";
        String ipAddress = request.getRemoteAddr() != null ?
                request.getRemoteAddr() : "";
        String acceptLanguage = request.getHeader("Accept-Language") != null ?
                request.getHeader("Accept-Language") : "";

        // Create a fingerprint hash
        return DigestUtils.sha256Hex(userAgent + ipAddress + acceptLanguage);
    }

    // Verify token (either access or refresh)
    private SignedJWT verifyToken(String token) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        if (!signedJWT.verify(verifier)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        Date expiryTime = claims.getExpirationTime();

        if (expiryTime == null || expiryTime.before(new Date())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String tokenType = claims.getStringClaim("tokenType");
        String jwtId = claims.getJWTID();

        // Check if access token is invalidated
        if ("access".equals(tokenType) && invalidatedTokenRepository.existsById(jwtId)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Check if refresh token is revoked
        if ("refresh".equals(tokenType)) {
            refreshTokenRepository.findByIdAndRevoked(jwtId, false)
                    .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        }

        return signedJWT;
    }

}

