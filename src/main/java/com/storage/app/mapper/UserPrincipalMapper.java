package com.storage.app.mapper;

import com.storage.app.dto.user.UserDto;
import com.storage.app.model.User;
import com.storage.app.security.UserPrincipal;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserPrincipalMapper {

    User toUser(UserDto userDto);

    UserPrincipal toUserPrincipal(User user);
}
