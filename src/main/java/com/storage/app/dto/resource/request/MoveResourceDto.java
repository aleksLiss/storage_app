package com.storage.app.dto.resource.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dto for move or rename resource")
public class MoveResourceDto {
    @Schema(description = "path from or old name of resource")
    @Size(min = 1, max = 1024, message = "Path length must be from 1 to 1024")
    private String from;
    @Schema(description = "path to or new name of resource")
    @Size(min = 1, max = 1024, message = "Path length must be from 1 to 1024")
    private String to;
}
