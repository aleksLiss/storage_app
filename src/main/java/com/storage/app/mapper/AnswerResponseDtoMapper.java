package com.storage.app.mapper;

import com.storage.app.dto.resource.response.AnswerResponseDto;
import com.storage.app.security.UserPrincipal;
import com.storage.app.util.resource.ResourceFinder;
import io.minio.Result;
import io.minio.messages.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AnswerResponseDtoMapper {

    @Mapping(target = "type", ignore = true)
    AnswerResponseDto toDto(Item item);

    default List<AnswerResponseDto> getListAnswerResponseDtos(Iterable<Result<Item>> results,
                                                              String resource,
                                                              @AuthenticationPrincipal UserPrincipal userDetails) {
        List<AnswerResponseDto> response = new ArrayList<>();
        try {
            for (Result<Item> result : results) {
                Item item = result.get();
                String fullPath = item.objectName();
                String normalizedResource = resource.endsWith("/") ? resource : resource + "/";
                if (fullPath.equals(normalizedResource)) continue;
                AnswerResponseDto answerDto = toDto(item);
                String name = ResourceFinder.getResourceNameFromPath(fullPath);
                String userRootPrefix = String.format("user-%s-files/", userDetails.getUserId());
                String relativePath = fullPath.startsWith(userRootPrefix)
                        ? fullPath.substring(userRootPrefix.length())
                        : fullPath;
                boolean isDirectory = fullPath.endsWith("/");
                answerDto.setName(name);
                answerDto.setPath(relativePath);
                answerDto.setType(isDirectory ? "DIRECTORY" : "FILE");
                response.add(answerDto);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Mapping error: " + ex.getMessage());
        }
        return response;
    }
}
