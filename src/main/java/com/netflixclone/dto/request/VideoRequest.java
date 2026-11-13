package com.netflixclone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;


@Data
public class VideoRequest {
    @NotBlank(message="title is required")
    private String title;
    @Size(max=4000,message="Description must not exceed 4000 characters")
    private String description;
    @Min(1888)
    @Max(2200)
    private Integer year;
    @Size(max = 20)
    private String rating;
    @Min(1)
    @Max(1000)
    private Integer duration;
    private boolean published;
    private List<String> categories;
}
