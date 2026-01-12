package com.example.bumil_backend.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @NotBlank
    private String email;

    @NotBlank
    private String name;

    @NotBlank
    private Integer studentNum;

    @NotBlank(message = "새비밀번호를 입력해주세요.")
    private String newPassword;

}
