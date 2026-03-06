package com.storage.app.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Dto for sign-up user")
public class UserDto {
    @Schema(name = "email", examples = "max@google.com")
    @NotBlank(message = "name must be not empty")
    @Length(message = "name must be not empty and has length 5 - 30", min=5, max=30)
    @Email
    private String username;
    @Schema(name = "password", examples = "123qwe")
    @NotBlank(message = "password must be not empty")
    @Length(message = "password must be not empty and has length 6 - 100", min=6, max=100)
    private String password;
}
