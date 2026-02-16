package com.storage.app.util.path;

import com.storage.app.dto.resource.request.SearchResourceDto;
import com.storage.app.dto.resource.response.AnswerResponseDto;
import com.storage.app.util.CopyAnswerResponseDtoCreator;
import io.minio.Result;
import io.minio.messages.Item;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

@Component
@Data
@Slf4j
public class PathParser {

    private final CopyAnswerResponseDtoCreator copyAnswerResponseDtoCreator;

    public HashSet<AnswerResponseDto> parsePath(Iterable<Result<Item>> results, @Valid SearchResourceDto searchResourceDto) {
        HashSet<AnswerResponseDto> resultDtos = new HashSet<>();
        String query = searchResourceDto.getQuery();
        Pattern pattern = Pattern.compile("(^|/)" + Pattern.quote(query) + "(\\.[^/]*|/|$)");
        try {
            for (Result<Item> result : results) {
                Item item = result.get();
                String fullPath = item.objectName();
                if (!pattern.matcher(fullPath).find()) {
                    continue;
                }
                String[] elementsOfPath = Arrays.copyOf(fullPath.split("/"), fullPath.split("/").length);
                AnswerResponseDto answerResponseDto = new AnswerResponseDto();
                for (String s1 : elementsOfPath) {
                    if (!s1.contains(".")) {
                        if (s1.equals(query)) {
                            if (answerResponseDto.getName() != null && !answerResponseDto.getName().isEmpty()) {
                                String prevPath = answerResponseDto.getPath();
                                String newPath = prevPath + s1;
                                answerResponseDto.setPath(newPath + "/");
                                answerResponseDto.setName(s1);
                            } else {
                                if (answerResponseDto.getPath() == null || answerResponseDto.getPath().isEmpty()) {
                                    answerResponseDto.setName(s1 + "/");
                                    answerResponseDto.setPath("");
                                } else {
                                    answerResponseDto.setName(s1 + "/");
                                }
                            }
                            resultDtos.add(copyAnswerResponseDtoCreator.copyOf(answerResponseDto));
                        } else {
                            if (answerResponseDto.getPath() == null || answerResponseDto.getPath().isEmpty()) {
                                if (answerResponseDto.getName() != null && !answerResponseDto.getName().isEmpty()) {
                                    String previousPath = answerResponseDto.getPath();
                                    if (previousPath.isEmpty()) {
                                        String previousResult = answerResponseDto.getName();
                                        String newPath = previousResult + s1 + "/";
                                        answerResponseDto.setPath(newPath);
                                    } else {
                                        String newPath = previousPath + s1 + "/";
                                        answerResponseDto.setPath(newPath);
                                    }
                                    answerResponseDto.setName("");
                                } else {
                                    answerResponseDto.setPath(s1 + "/");
                                }
                            } else {
                                answerResponseDto.setPath(answerResponseDto.getPath() + s1 + "/");
                            }
                        }
                    }
                    if (s1.contains(".")) {
                        if (s1.startsWith(query)) {
                            if (answerResponseDto.getName() != null && !answerResponseDto.getName().isEmpty()) {
                                String previousResult = answerResponseDto.getName();
                                String previousPath = answerResponseDto.getPath();
                                String newPath = previousPath + previousResult;
                                answerResponseDto.setPath(newPath);
                                answerResponseDto.setName(s1);
                            } else {
                                answerResponseDto.setName(s1);
                            }
                            resultDtos.add(answerResponseDto);
                        }
                    }
                }
                for (AnswerResponseDto answerDto : resultDtos) {
                    if (answerDto.getName().endsWith("/")) {
                        answerDto.setType("DIRECTORY");
                    } else {
                        answerDto.setSize(String.valueOf(item.size()));
                        answerDto.setType("FILE");
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Exception occurred while parsing path", ex);
        }
        return resultDtos;
    }
}