package com.storage.app.dto.resource.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UploadResourceDto {

    @NotBlank(message = "path must be not empty")
    @Size(min = 1, max = 1024, message = "length path must be from 1 to 1024")
    @Pattern(
            regexp = "^([^/]+/)+$",
            message = "Name folder should be as in the example: folder/"
    )
    private String path;
}
