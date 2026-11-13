package com.netflixclone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message="Current Password is required")
    private String currentPassword;
    @NotBlank(message="New Password is required")
    @Size(min = 8, max = 72)
    private String newPassword;

}
