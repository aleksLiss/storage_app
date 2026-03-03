package com.storage.app.dto.resource.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UploadResourceDto {
    @Size(min = 1, max = 1024, message = "Path length must be from 1 to 1024")
    private String path;
}

