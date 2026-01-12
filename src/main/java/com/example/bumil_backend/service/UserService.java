package com.example.bumil_backend.service;

import com.example.bumil_backend.common.exception.BadRequestException;
import com.example.bumil_backend.common.exception.ResourceNotFoundException;
import com.example.bumil_backend.dto.user.request.UserPasswordUpdateRequest;
import com.example.bumil_backend.dto.user.request.UserUpdateRequest;
import com.example.bumil_backend.dto.user.response.UpdateUserPasswordResponse;
import com.example.bumil_backend.dto.user.response.UserDetailResponse;
import com.example.bumil_backend.dto.user.response.UserUpdateResponse;
import com.example.bumil_backend.entity.Users;
import com.example.bumil_backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        Users user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("유저가 존재하지 않습니다."));

        return UserDetailResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .studentNum(user.getStudentNum())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public UserUpdateResponse updateUser(UserUpdateRequest request) {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        Users user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("유저가 존재하지 않습니다."));

        user.updateInfo(
                request.getEmail(),
                request.getName(),
                request.getStudentNum()
        );

        return UserUpdateResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .studentNum(user.getStudentNum())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public UpdateUserPasswordResponse updateUserPassword(UserPasswordUpdateRequest request) {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        Users user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("유저가 존재하지 않습니다."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("기존 비밀번호가 일치하지 않습니다.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        String newHashedPassword = passwordEncoder.encode(request.getNewPassword());

        user.updatePassword(newHashedPassword);

        return UpdateUserPasswordResponse.builder()
                .userId(user.getId())
                .build();
    }
}
