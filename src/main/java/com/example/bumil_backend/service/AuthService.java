package com.example.bumil_backend.service;

import com.example.bumil_backend.common.exception.InvalidTokenException;
import com.example.bumil_backend.common.exception.NotAcceptableUserException;
import com.example.bumil_backend.common.exception.NotLoggedInException;
import com.example.bumil_backend.common.exception.UserAlreadyExistException;
import com.example.bumil_backend.config.TokenProvider;
import com.example.bumil_backend.dto.refreshToken.RefreshDto;
import com.example.bumil_backend.dto.refreshToken.request.RefreshRequest;
import com.example.bumil_backend.dto.refreshToken.response.RefreshResponse;
import com.example.bumil_backend.dto.user.request.DeleteUserRequest;
import com.example.bumil_backend.dto.user.request.LoginRequest;
import com.example.bumil_backend.dto.user.request.LogoutRequest;
import com.example.bumil_backend.dto.user.request.SignupRequest;
import com.example.bumil_backend.dto.user.response.LoginResponse;
import com.example.bumil_backend.dto.user.response.SignupResponse;
import com.example.bumil_backend.entity.RefreshToken;
import com.example.bumil_backend.entity.Role;
import com.example.bumil_backend.entity.Users;
import com.example.bumil_backend.repository.RefreshTokenRepository;
import com.example.bumil_backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;


    public SignupResponse signup(SignupRequest request){
        if(userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())){
            throw new UserAlreadyExistException("이미 가입된 이메일입니다.");
        }

        Users user = Users.builder()
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .email(request.getEmail())
                .build();

        userRepository.save(user);

        return SignupResponse.builder()
                .userId(user.getId())
                .build();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // ID, PW 검증
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );


        Users user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User doesn't exist"));

        String accessToken = tokenProvider.createAccessToken(user.getEmail(), String.valueOf(user.getRole()));

        String refreshToken = tokenProvider.createRefreshToken(request.getEmail(), String.valueOf(user.getRole()));

        RefreshToken saveRefreshToken = RefreshToken.builder()
                .token(refreshToken)
                .expiryDatetime(LocalDateTime.now().plusDays(1))
                .user(user)
                .build();

        refreshTokenRepository.save(saveRefreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public RefreshResponse refresh(RefreshRequest requestDto){

        RefreshDto existingRefreshToken = refreshTokenRepository.findByToken(requestDto.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("토큰을 찾을 수 없습니다."));

        if(existingRefreshToken.getExpiryDatetime().isBefore(LocalDateTime.now())){
            refreshTokenRepository.deleteByToken(requestDto.getRefreshToken());
            throw new InvalidTokenException("토큰이 만료되었습니다.");
        }

        String username = tokenProvider.extractUsername(existingRefreshToken.getToken());
        String role = tokenProvider.extractRole(existingRefreshToken.getToken());
        String newAccessToken = tokenProvider.createAccessToken(username, role);

        return RefreshResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }

    @Transactional
    public void logout(HttpServletRequest httpServletRequest, LogoutRequest request) {
        String accessToken = extractAccessToken(httpServletRequest);
        if (accessToken == null) {
            throw new NotLoggedInException("로그인이 필요한 요청입니다.");
        }

        if (!tokenProvider.validateToken(accessToken)) {
            throw new InvalidTokenException("유효하지 않은 Access Token입니다.");
        }

        String refreshToken = request.getRefreshToken();
        if (refreshToken != null && !refreshToken.isEmpty()) {
            refreshTokenRepository.deleteByToken(refreshToken);
        }
    }

    @Transactional
    public void deleteUser(HttpServletRequest httpServletRequest, DeleteUserRequest request) {
        String bearerToken = httpServletRequest.getHeader("Authorization");
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new InvalidTokenException("토큰 형식이 잘못되었거나 유효하지 않습니다.");
        }
        String accessToken = bearerToken.substring(7);

        if (!tokenProvider.validateToken(accessToken)) {
            throw new InvalidTokenException("유효하지 않거나 만료된 Access Token입니다.");
        }

        String email = tokenProvider.extractUsername(accessToken);

        Users user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new UsernameNotFoundException("유저가 존재하지 않습니다."));

        RefreshDto refreshDto = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("토큰이 존재하지 않습니다."));

        if (!refreshDto.getUserId().equals(user.getId())) {
            throw new NotAcceptableUserException("요청에 포함된 Refresh Token이 현재 유저의 토큰이 아닙니다.");
        }

        refreshTokenRepository.deleteByToken(request.getRefreshToken());

        user.delete();
        userRepository.save(user);
    }

    private String extractAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 제거
        }

        return null;
    }
}
