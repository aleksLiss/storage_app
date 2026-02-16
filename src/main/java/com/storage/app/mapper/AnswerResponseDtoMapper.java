package com.storage.app.mapper;

import com.storage.app.dto.resource.response.AnswerResponseDto;
import com.storage.app.util.resource.ResourceFinder;
import io.minio.Result;
import io.minio.messages.Item;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AnswerResponseDtoMapper {

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
                                                                          String resource) throws Exception {
        List<LinkedHashMap<String, String>> response = new ArrayList<>();
        for (Result<Item> result : results) {
            LinkedHashMap<String, String> map;
            Item item = result.get();
            String fullPath = item.objectName();
            if (fullPath.equals(resource)) {
                continue;
            }
            String path = resourceFinder.getPathToResource(fullPath);
            String name = resourceFinder.getResourceName(fullPath);
            long size = item.size();
            String type = item.size() == 0 ? "DIRECTORY" : "FILE";
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
}
