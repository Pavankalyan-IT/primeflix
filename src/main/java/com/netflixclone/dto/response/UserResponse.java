package com.netflixclone.dto.response;

import com.netflixclone.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private long id;
    private String email;
    private String fullname;
    private String role;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;


    public static UserResponse fromEntity(User user){
        return new UserResponse(user.getId(),
                user.getEmail(),
                user.getFullname(),
                user.getRole().name(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt());

    }
}
