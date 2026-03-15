package com.storage.app.service;

import com.storage.app.dto.resource.request.CreateFolderDto;
import com.storage.app.dto.resource.request.FoundResourceDto;
import com.storage.app.dto.resource.response.AnswerResponseDto;
import com.storage.app.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

public interface MinioFolderService extends MinioBaseService {

    StreamingResponseBody downloadFolder(@Valid @ModelAttribute FoundResourceDto foundResourceDto,
                                         UserPrincipal userDetails);

    List<@NonNull AnswerResponseDto> getResourceFromDirectory(UserPrincipal userDetails,
                                                                                       @Valid @ModelAttribute FoundResourceDto foundResourceDto);

    AnswerResponseDto createFolder(UserPrincipal userDetails, @Valid @ModelAttribute CreateFolderDto folderDto);

    void createRootFolderForUserByUsername(UserPrincipal userPrincipal);
}
