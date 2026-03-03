package com.storage.app.dto.resource.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SearchResourceDto {
    @NotBlank(message = "Path must be not empty")
    @Size(min = 1, max = 1024, message = "Path length must be from 1 to 50")
    public  String query;
}
