package com.storage.app.dto.resource.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Dto for delete | download | search resources")
public class FoundResourceDto {
    @Schema(description = "path to resource", examples = "folder1/folder2/")
    @Size(max = 1024, message = "length path must be from 1 to 1024")
    private String path;
}
