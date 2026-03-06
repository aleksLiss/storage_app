package com.storage.app.mapper;

import com.storage.app.dto.resource.response.AnswerResponseDto;
import com.storage.app.exception.mapper.AnswerResponseDtoMapperException;
import com.storage.app.util.resource.ResourceFinder;
import io.minio.Result;
import io.minio.messages.Item;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AnswerResponseDtoMapper {

    default List<LinkedHashMap<String, String>> getListAnswerResponseDtos(Iterable<Result<Item>> results,
                                                                          @Context ResourceFinder resourceFinder,
                                                                          String resource) {
        List<LinkedHashMap<String, String>> response = new ArrayList<>();
        try {
            for (Result<Item> result : results) {
                Item item = result.get();
                String fullPath = item.objectName();
                String normalizedResource = resource.endsWith("/") ? resource : resource + "/";
                if (fullPath.equals(normalizedResource)) {
                    continue;
                }
                boolean isDir = fullPath.endsWith("/");
                String name = resourceFinder.getResourceNameFromPath(fullPath);
                String path = resourceFinder.getPathToResourceFromPath(fullPath);
                AnswerResponseDto answerDto = new AnswerResponseDto();
                answerDto.setPath(path);
                if (isDir) {
                    answerDto.setName(name + "/");
                    answerDto.setType("DIRECTORY");
                } else {
                    answerDto.setName(name);
                    answerDto.setType("FILE");
                    answerDto.setSize(Long.toString(item.size()));
                }
                response.add(answerResponseDtoToMap(answerDto));
            }
        } catch (Exception ex) {
            throw new AnswerResponseDtoMapperException("Error map item to answer response dto: " + ex.getMessage());
        }
        return response;
    }

    default LinkedHashMap<String, String> answerResponseDtoToMap(AnswerResponseDto answerResponseDto) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("path", answerResponseDto.getPath());
        map.put("name", answerResponseDto.getName());
        if (answerResponseDto.getSize() != null) {
            map.put("size", answerResponseDto.getSize());
        }
        map.put("type", answerResponseDto.getType());
        return map;
    }

    default LinkedHashMap<String, String> answerResponseDtoToMap(@Context ResourceFinder resourceFinder,
                                                                 String pathToFolder) {
        AnswerResponseDto answerResponseDto = new AnswerResponseDto();
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        String path = resourceFinder.getPathToResourceFromPath(pathToFolder);
        String name = resourceFinder.getResourceNameFromPath(pathToFolder);
        answerResponseDto.setPath(path);
        answerResponseDto.setName(name);
        if (name.endsWith("/")) {
            answerResponseDto.setType("DIRECTORY");
        }
        map.put("path", answerResponseDto.getPath());
        map.put("name", answerResponseDto.getName());
        if (answerResponseDto.getSize() != null) {
            map.put("size", answerResponseDto.getSize());
        }
        if (answerResponseDto.getSize() == null) {
            answerResponseDto.setType("DIRECTORY");
        }
        map.put("type", answerResponseDto.getType());
        return map;
    }
}
