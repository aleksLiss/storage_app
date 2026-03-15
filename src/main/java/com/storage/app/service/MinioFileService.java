package com.storage.app.service;

import com.storage.app.dto.resource.request.FoundResourceDto;
import com.storage.app.dto.resource.request.UploadResourceDto;
import com.storage.app.dto.resource.response.AnswerResponseDto;
import com.storage.app.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

public interface MinioFileService extends MinioBaseService {

    StreamingResponseBody downloadFile(FoundResourceDto foundResourceDto, UserPrincipal userDetails);

    List<@NonNull AnswerResponseDto> uploadResource(@ModelAttribute UploadResourceDto uploadResourceDto,
                                                    @RequestParam("object") MultipartFile[] files,
                                                    UserPrincipal userDetails);

    AnswerResponseDto getResource(@Valid @ModelAttribute FoundResourceDto foundResourceDto,
                                  UserPrincipal userDetails);
}
