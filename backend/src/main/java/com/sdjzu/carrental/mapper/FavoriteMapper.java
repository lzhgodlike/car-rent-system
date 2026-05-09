package com.sdjzu.carrental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdjzu.carrental.model.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
