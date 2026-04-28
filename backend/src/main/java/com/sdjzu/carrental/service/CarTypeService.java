package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdjzu.carrental.mapper.CarTypeMapper;
import com.sdjzu.carrental.model.entity.CarType;
import com.sdjzu.carrental.model.request.CarTypeRequest;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarTypeService {

    private final CarTypeMapper carTypeMapper;

    public CarTypeService(CarTypeMapper carTypeMapper) {
        this.carTypeMapper = carTypeMapper;
    }

    public List<CarType> list() {
        return carTypeMapper.selectList(new LambdaQueryWrapper<CarType>().orderByAsc(CarType::getId));
    }

    public void add(CarTypeRequest request) {
        SecurityUtils.requireAdmin();
        CarType carType = new CarType();
        BeanUtils.copyProperties(request, carType);
        carTypeMapper.insert(carType);
    }

    public void update(Long id, CarTypeRequest request) {
        SecurityUtils.requireAdmin();
        CarType carType = new CarType();
        BeanUtils.copyProperties(request, carType);
        carType.setId(id);
        carTypeMapper.updateById(carType);
    }

    public void delete(Long id) {
        SecurityUtils.requireAdmin();
        carTypeMapper.deleteById(id);
    }
}
