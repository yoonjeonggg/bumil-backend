package com.example.bumil_backend.controller;

import com.example.bumil_backend.common.ApiResponse;
import com.example.bumil_backend.dto.chat.request.UserUpdateForAdminRequest;
import com.example.bumil_backend.dto.chat.response.ChatListDto;
import com.example.bumil_backend.dto.user.response.GetAllUsersResponse;
import com.example.bumil_backend.dto.user.response.UserUpdateResponse;
import com.example.bumil_backend.enums.ChatTags;
import com.example.bumil_backend.enums.DateFilter;
import com.example.bumil_backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Tag(name = "Admin Controller", description = "Administrator Controller API")
public class AdminController {

    private final AdminService adminService;

    @PatchMapping("/exit/{userId}")
    @Operation(summary = "강제 회원 탈퇴", description = "강제 회원 탈퇴 시 사용하는 API 입니다.")
    public ResponseEntity<ApiResponse<Void>> adminDeleteUser(@PathVariable Long userId) {
        adminService.adminDeleteUser(userId);
        return ApiResponse.ok("강제 회원 탈퇴되었습니다.");
    }

    @GetMapping("/chats")
    @Operation(summary = "모든 채팅 목록 조회", description = "태그와 날짜정렬로 필터링 할 수 있습니다.")
    public ResponseEntity<ApiResponse<Page<ChatListDto>>> getChats
            (@RequestParam(defaultValue = "RECENT") DateFilter dateFilter,
             @RequestParam(required = false) ChatTags chatTags,
             @RequestParam(required = false) Pageable pageable
            ) {
        Page<ChatListDto> result = adminService.getChats(dateFilter, chatTags, pageable);
        return ApiResponse.ok(result, "조회되었습니다.");
    }

    @PatchMapping("/{userId}")
    @Operation(summary = "회원 정보 수정", description = "회원 정보 시 사용하는 API 입니다.")
    public ResponseEntity<ApiResponse<UserUpdateResponse>> patchUser
            (@PathVariable Long userId, @RequestBody UserUpdateForAdminRequest request) {
        UserUpdateResponse result = adminService.patchUser(userId, request);
        return ApiResponse.ok(result, "수정되었습니다.");
    }

    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "모든 유저 조회 API")
    public ResponseEntity<ApiResponse<List<GetAllUsersResponse>>> getAllUsers(){
        return ApiResponse.ok(adminService.getAllUsers(), "모든 유저 조회에 성공하였습니다.");
    }

}