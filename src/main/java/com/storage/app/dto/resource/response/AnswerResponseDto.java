package com.storage.app.dto.resource.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"path", "name"})
public class AnswerResponseDto {
    private String path;
    private String name;
    private String size;
    private String type;
}
