package com.example.bumil_backend.dto.chat.request;

import lombok.Data;

@Data
public class UserUpdateForAdminRequest {
    private String email;

    private String name;

    private Integer studentNum;

    private String newPassword;
}
