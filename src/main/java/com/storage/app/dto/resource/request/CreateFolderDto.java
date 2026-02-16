package com.storage.app.dto.resource.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFolderDto {

    @NotBlank(message = "path must be not empty")
    @Size(min = 1, max = 1024, message = "length path must be from 1 to 1024")
    @Pattern(
            regexp = "^([a-zA-Z0-9]+/)+$",
            message = "Folder name must contain only letters and numbers and end with a slash (e.g., folder/ or f1/f2/)"
    )
    private String path;
}
