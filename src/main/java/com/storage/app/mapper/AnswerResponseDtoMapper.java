package com.storage.app.mapper;

import com.storage.app.dto.resource.response.AnswerResponseDto;
import com.storage.app.exception.mapper.AnswerResponseDtoMapperException;
import com.storage.app.util.resource.ResourceFinder;
import io.minio.Result;
import io.minio.messages.Item;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AnswerResponseDtoMapper {

    Logger log = LoggerFactory.getLogger(AnswerResponseDtoMapper.class);

    default List<LinkedHashMap<String, String>> hashsetSearchAnswerDtoToMap(HashSet<AnswerResponseDto> hashSet) {
        List<LinkedHashMap<String, String>> response = new ArrayList<>();
        for (AnswerResponseDto answerResponseDto : hashSet) {
            LinkedHashMap<String, String> map = answerResponseDtoToMap(answerResponseDto);
            response.add(map);
        }
        return response;
    }

    default List<LinkedHashMap<String, String>> getListAnswerResponseDtos(Iterable<Result<Item>> results,
                                                                          @Context ResourceFinder resourceFinder,
                                                                          String resource) {
        List<LinkedHashMap<String, String>> response = new ArrayList<>();
        LinkedHashMap<String, String> map;
        try {
            for (Result<Item> result : results) {
                Item item = result.get();
                String fullPath = item.objectName();
                if (fullPath.equals(resource)) {
                    continue;
                }
                String path = resourceFinder.getPathToResource(fullPath);
                long size = item.size();
                String name = size == 0
                        ? resourceFinder.getResourceName(fullPath) + "/"
                        : resourceFinder.getResourceName(fullPath);
                String type = size == 0 ? "DIRECTORY" : "FILE";
                AnswerResponseDto answerDto = new AnswerResponseDto();
                answerDto.setPath(path);
                answerDto.setName(name);
                if (size != 0) {
                    answerDto.setSize(Long.toString(size));
                }
                answerDto.setType(type);
                map = answerResponseDtoToMap(answerDto);
                response.add(map);
            }
        } catch (Exception ex) {
            throw new AnswerResponseDtoMapperException("Error map item to answer response dto");
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
//                                                                 CreateFolderDto folderDto
                                                                 String pathToFolder) {
        AnswerResponseDto answerResponseDto = new AnswerResponseDto();
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
//        String path = resourceFinder.getPathToResource(folderDto.getPath());
        String path = resourceFinder.getPathToResource(pathToFolder);
//        String name = resourceFinder.getResourceName(folderDto.getPath());
        String name = resourceFinder.getResourceName(pathToFolder);
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
        map.put("type", answerResponseDto.getType());
        return map;
    }
}
