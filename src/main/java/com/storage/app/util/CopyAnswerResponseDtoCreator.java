package com.storage.app.util;

import com.storage.app.dto.resource.response.AnswerResponseDto;
import org.springframework.stereotype.Component;

@Component
public class CopyAnswerResponseDtoCreator {

    public AnswerResponseDto copyOf(AnswerResponseDto other) {
        if (other == null) return null;
        AnswerResponseDto answerResponseDto = new AnswerResponseDto();
        answerResponseDto.setName(other.getName());
        answerResponseDto.setPath(other.getPath());
        return answerResponseDto;
    }
}
