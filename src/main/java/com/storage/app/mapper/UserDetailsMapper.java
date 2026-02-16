package com.storage.app.mapper;

import com.storage.app.dto.user.UserDetailsImpl;
import com.storage.app.dto.user.UserDto;
import com.storage.app.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserDetailsMapper {

    User toUser(UserDto userDto);

    UserDetailsImpl toUserDetails(User user);
}
