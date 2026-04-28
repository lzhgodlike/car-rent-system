package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.mapper.CarMapper;
import com.sdjzu.carrental.model.entity.Car;
import com.sdjzu.carrental.model.request.CarRequest;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CarService {

    private final CarMapper carMapper;

    public CarService(CarMapper carMapper) {
        this.carMapper = carMapper;
    }

    public List<Car> list(String brand, Long typeId, String status) {
        LambdaQueryWrapper<Car> wrapper = new LambdaQueryWrapper<Car>()
                .like(StringUtils.hasText(brand), Car::getBrand, brand)
                .eq(typeId != null, Car::getTypeId, typeId)
                .eq(StringUtils.hasText(status), Car::getStatus, status)
                .orderByDesc(Car::getId);
        return carMapper.selectList(wrapper);
    }

    public Car detail(Long id) {
        Car car = carMapper.selectById(id);
        if (car == null) {
            throw new BusinessException("车辆不存在");
        }
        return car;
    }

    public void add(CarRequest request) {
        SecurityUtils.requireAdmin();
        Car car = new Car();
        BeanUtils.copyProperties(request, car);
        if (!StringUtils.hasText(car.getStatus())) {
            car.setStatus("AVAILABLE");
        }
        carMapper.insert(car);
    }

    public void update(Long id, CarRequest request) {
        SecurityUtils.requireAdmin();
        Car car = new Car();
        BeanUtils.copyProperties(request, car);
        car.setId(id);
        carMapper.updateById(car);
    }

    public void delete(Long id) {
        SecurityUtils.requireAdmin();
        carMapper.deleteById(id);
    }
}
