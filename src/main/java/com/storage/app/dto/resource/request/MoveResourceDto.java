package com.storage.app.dto.resource.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MoveResourceDto {

    @NotBlank
    @Size(min = 1, max = 1024, message = "length path must be from 1 to 1024")
    private String from;
    @NotBlank
    @Size(min = 1, max = 1024, message = "length path must be from 1 to 1024")
    private String to;
}
