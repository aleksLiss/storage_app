package com.storage.app.dto.resource.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FoundResourceDto {
    @Size(max = 1024, message = "length path must be from 1 to 1024")
    private String path;
}
