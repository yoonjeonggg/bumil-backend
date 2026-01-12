package com.example.bumil_backend.dto.user.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GetAllUsersResponse {
    private Long userId;
    private String email;
    private String name;
    private Integer studentNum;
}
