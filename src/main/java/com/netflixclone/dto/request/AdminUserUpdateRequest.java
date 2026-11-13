package com.netflixclone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
        @NotBlank @Size(max = 255) String fullName) {
}
