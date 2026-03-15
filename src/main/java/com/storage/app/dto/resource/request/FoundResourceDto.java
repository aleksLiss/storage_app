package com.storage.app.dto.resource.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Dto for delete | download | search resources")
public class FoundResourceDto {
    @Schema(description = "path to resource", examples = "folder1/folder2/")
    @NotBlank(message = "Path must be not empty")
    @Size(max = 1024, message = "length path must be from 1 to 1024")
    private String path;
}
