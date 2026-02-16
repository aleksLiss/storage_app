package com.storage.app.dto.resource.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SearchResourceDto {
    @NotBlank
    @Size(min = 1, max = 1024, message = "length path must be from 1 to 1024")
    public  String query;
}
