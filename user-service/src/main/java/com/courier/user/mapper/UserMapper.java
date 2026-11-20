package com.courier.user.mapper;

import com.courier.user.dto.UserResponse;
import com.courier.user.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
