package com.storage.app.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    @NotBlank
    @Length(message = "name must be not empty and has length 3 - 30", min=3, max=30)
    @Email
    private String username;
    @NotBlank @Length(message = "password must be not empty and has length 8 - 100", min=8, max=100)
    private String password;
}
