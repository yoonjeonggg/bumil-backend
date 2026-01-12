package com.example.bumil_backend.dto.user.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserDetailResponse {
    private Long userId;
    private String email;
    private String name;
    private Integer studentNum;
    private LocalDateTime createdAt;
}
