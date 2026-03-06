package com.storage.app.util.path;

import com.storage.app.dto.resource.response.AnswerResponseDto;
import com.storage.app.util.CopyAnswerResponseDtoCreator;
import com.storage.app.util.resource.ResourceFinder;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Data
@Slf4j
public class PathParser {

    private final CopyAnswerResponseDtoCreator copyAnswerResponseDtoCreator;
    private final ResourceFinder resourceFinder;

    public List<AnswerResponseDto> parsePath(Iterable<Result<Item>> results) {
        List<AnswerResponseDto> answerResponseDtoList = new ArrayList<>();
        try {
            for (Result<Item> result : results) {
                Item item = result.get();
                String fullPath = item.objectName();
                String pathToResource = String.join(
                        "/",
                        Arrays.copyOfRange(fullPath.split("/"), 1, fullPath.split("/").length - 1))
                        + "/";
                if (pathToResource.equals("/")) {
                    pathToResource = "";
                }
                String lastElem = resourceFinder.getResourceNameFromPath(fullPath);
                AnswerResponseDto answerResponseDto = new AnswerResponseDto();
                if (lastElem.contains(".")) {
                    answerResponseDto.setPath(pathToResource);
                    answerResponseDto.setName(lastElem);
                    answerResponseDto.setSize(String.valueOf(item.size()));
                    answerResponseDto.setType("FILE");
                    answerResponseDtoList.add(answerResponseDto);
                    continue;
                }
                answerResponseDto.setPath(pathToResource);
                answerResponseDto.setName(lastElem + "/");
                answerResponseDto.setType("DIRECTORY");
                answerResponseDtoList.add(answerResponseDto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return answerResponseDtoList;
    }
}