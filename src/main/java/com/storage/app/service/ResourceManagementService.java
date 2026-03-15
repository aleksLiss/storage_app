package com.storage.app.service;

import com.storage.app.dto.resource.request.FoundResourceDto;
import com.storage.app.dto.resource.request.MoveResourceDto;
import com.storage.app.dto.resource.request.SearchResourceDto;
import com.storage.app.dto.resource.response.AnswerResponseDto;
import com.storage.app.security.UserPrincipal;

import java.security.Principal;
import java.util.List;

public interface ResourceManagementService extends MinioBaseService {

    List<AnswerResponseDto> searchResource(SearchResourceDto searchResourceDto, UserPrincipal userDetails);

    AnswerResponseDto moveResource(MoveResourceDto movedResourceDto, UserPrincipal userDetails);

    void deleteResource(FoundResourceDto foundResourceDto, UserPrincipal userDetails);

}
