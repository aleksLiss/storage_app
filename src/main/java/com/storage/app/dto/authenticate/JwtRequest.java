package com.storage.app.dto.authenticate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dto for sign-in user")
public record JwtRequest(

        @Schema(description = "Username", examples = "max@google.com")
        @NotBlank(message = "Username must be not empty")
        String username,
        @Schema(description = "Password", examples = "123qwe234!!))")
        @NotBlank(message = "Password must be not empty")
        @Size(min = 6, message = "Password length must be great than 6")
        String password
)
{ }
