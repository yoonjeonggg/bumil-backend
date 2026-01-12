package com.example.bumil_backend.service;


import com.example.bumil_backend.common.exception.BadRequestException;
import com.example.bumil_backend.dto.chat.request.UserUpdateForAdminRequest;
import com.example.bumil_backend.dto.chat.response.ChatListDto;
import com.example.bumil_backend.dto.user.response.GetAllUsersResponse;
import com.example.bumil_backend.dto.user.response.UserUpdateResponse;
import com.example.bumil_backend.entity.ChatRoom;
import com.example.bumil_backend.entity.Users;
import com.example.bumil_backend.enums.ChatTags;
import com.example.bumil_backend.enums.DateFilter;
import com.example.bumil_backend.repository.ChatRoomRepository;
import com.example.bumil_backend.repository.UserRepository;
import com.example.bumil_backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final ChatRoomRepository chatRoomRepository;
    private final PasswordEncoder passwordEncoder;

    // 강제 회원 탈퇴
    @Transactional
    public void adminDeleteUser(Long userId) {
        securityUtils.getCurrentAdmin();
        Users deleteUser = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 유저입니다."));

        deleteUser.setDeleted(true);
        userRepository.save(deleteUser);
    }


    // 모든 채팅 목록 조회
    public Page<ChatListDto> getChats(DateFilter dateFilter, ChatTags chatTags, Pageable pageable) {
        securityUtils.getCurrentAdmin();

        // 날짜별 정렬
        Sort sort = switch (dateFilter) {
            case RECENT -> Sort.by(Sort.Direction.DESC, "createdAt");
            case OLDEST -> Sort.by(Sort.Direction.ASC, "createdAt");
        };

        Pageable sortedPageable;
        if (pageable != null) {
            sortedPageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    sort
            );
        } else {
            // 기본 페이지 - 0페이지 10개
            sortedPageable = PageRequest.of(0, 10, sort);
        }

        Page<ChatRoom> chatRooms;

        // 태그별 필터링
        if (chatTags != null) {
            chatRooms = chatRoomRepository
                    .findAllByTagAndIsDeletedFalse(chatTags, sortedPageable);
        } else {
            chatRooms = chatRoomRepository
                    .findAllByIsDeletedFalse(sortedPageable);
        }

        return chatRooms.map(ChatListDto::from);
    }

    // 회원 정보 수정
    @Transactional
    public UserUpdateResponse patchUser(Long userId, UserUpdateForAdminRequest request) {
        securityUtils.getCurrentAdmin();

        Users patchUser = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 유저입니다."));

        if (request.getEmail() != null) {
            patchUser.setEmail(request.getEmail());
        }
        if (request.getName() != null) {
            patchUser.setName(request.getName());
        }

        if(request.getNewPassword() != null){
            if (passwordEncoder.matches(request.getNewPassword(), patchUser.getPassword())) {
                throw new BadRequestException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
            }

            String newHashedPassword = passwordEncoder.encode(request.getNewPassword());

            patchUser.updatePassword(newHashedPassword);
        }

        userRepository.save(patchUser);
        return UserUpdateResponse.from(patchUser);
    }

    @Transactional(readOnly = true)
    public List<GetAllUsersResponse> getAllUsers() {
        securityUtils.getCurrentAdmin();

        List<Users> users = userRepository.findAllByIsDeletedFalse();

        return users.stream()
                .map(user -> GetAllUsersResponse.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .studentNum(user.getStudentNum())
                        .build()
                )
                .toList();
    }
}
