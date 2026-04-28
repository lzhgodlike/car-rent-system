package com.sdjzu.carrental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdjzu.carrental.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
