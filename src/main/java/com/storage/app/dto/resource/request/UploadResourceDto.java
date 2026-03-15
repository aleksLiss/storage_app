package com.storage.app.dto.resource.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dto for upload resources")
public class UploadResourceDto {
    @Schema(description = "path to resource", example = "folder1/folder2/")
    @Size(min = 1, max = 1024, message = "Path length must be from 1 to 1024")
    @NotBlank(message = "Path must be not empty")
    private String path;
}

