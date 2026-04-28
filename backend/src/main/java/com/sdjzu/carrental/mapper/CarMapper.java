package com.sdjzu.carrental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdjzu.carrental.model.entity.Car;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CarMapper extends BaseMapper<Car> {
}
