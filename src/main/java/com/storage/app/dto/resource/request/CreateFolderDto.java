package com.storage.app.dto.resource.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dto for create folder")
public class CreateFolderDto {

    @Schema(description = "Path to resource", examples = "folder1/folder2/")
    @NotBlank(message = "path must be not empty")
    @Size(min = 1, max = 1024, message = "length path must be from 1 to 1024")
    private String path;
}
