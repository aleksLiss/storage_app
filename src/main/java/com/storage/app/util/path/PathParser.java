package com.storage.app.util.path;

import com.storage.app.dto.resource.response.AnswerResponseDto;
import com.storage.app.util.resource.ResourceFinder;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class PathParser {

    public static List<AnswerResponseDto> parsePath(Iterable<Result<Item>> results) {
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
                boolean isDirectory = item.objectName().endsWith("/");
                String lastElem = ResourceFinder.getResourceNameFromPath(fullPath);
                AnswerResponseDto answerResponseDto = new AnswerResponseDto();
                if (!isDirectory) {
                    answerResponseDto.setPath(pathToResource);
                    answerResponseDto.setName(lastElem);
                    answerResponseDto.setSize(item.size());
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