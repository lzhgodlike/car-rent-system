package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.common.PageResult;
import com.sdjzu.carrental.mapper.CarMapper;
import com.sdjzu.carrental.model.entity.Car;
import com.sdjzu.carrental.model.request.CarRequest;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CarService {

    private final CarMapper carMapper;

    public CarService(CarMapper carMapper) {
        this.carMapper = carMapper;
    }

    public List<String> listBrands() {
        return carMapper.selectList(new LambdaQueryWrapper<Car>().select(Car::getBrand).groupBy(Car::getBrand))
                .stream().map(Car::getBrand).collect(Collectors.toList());
    }

    public PageResult<Car> list(String brand, Long typeId, String status, String sort, int pageNum, int pageSize) {
        Page<Car> page;
        if ("rentCount".equals(sort) || "totalIncome".equals(sort)) {
            String orderCol = "rentCount".equals(sort) ? "s.cnt" : "s.income";
            int offset = (pageNum - 1) * pageSize;
            long total = carMapper.selectFilteredCount(brand, typeId, status);
            List<Car> cars = carMapper.selectWithRentalStats(brand, typeId, status, orderCol, "DESC", offset, pageSize);
            page = new Page<>(pageNum, pageSize, total);
            page.setRecords(cars);
        } else {
            LambdaQueryWrapper<Car> wrapper = new LambdaQueryWrapper<Car>()
                    .like(StringUtils.hasText(brand), Car::getBrand, brand)
                    .eq(typeId != null, Car::getTypeId, typeId)
                    .eq(StringUtils.hasText(status), Car::getStatus, status);
            if ("asc".equals(sort)) {
                wrapper.orderByAsc(Car::getDayPrice);
            } else if ("desc".equals(sort)) {
                wrapper.orderByDesc(Car::getDayPrice);
            } else {
                wrapper.orderByDesc(Car::getId);
            }
            page = carMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        }
        enrichWithRentalStats(page.getRecords());
        PageResult<Car> result = PageResult.of(page);
        result.summary("totalCount", carMapper.selectCount(null));
        result.summary("available", carMapper.selectCount(new LambdaQueryWrapper<Car>().eq(Car::getStatus, "AVAILABLE")));
        result.summary("rented", carMapper.selectCount(new LambdaQueryWrapper<Car>().eq(Car::getStatus, "RENTED")));
        result.summary("maintenance", carMapper.selectCount(new LambdaQueryWrapper<Car>().eq(Car::getStatus, "MAINTENANCE")));
        return result;
    }

    private void enrichWithRentalStats(List<Car> cars) {
        if (cars == null || cars.isEmpty()) return;
        Map<Long, Map<String, Object>> statsMap = carMapper.selectRentalStats().stream()
                .collect(Collectors.toMap(m -> ((Number) m.get("carId")).longValue(), m -> m, (a, b) -> a));
        for (Car car : cars) {
            Map<String, Object> stats = statsMap.get(car.getId());
            if (stats != null) {
                car.setRentCount(((Number) stats.get("cnt")).intValue());
                car.setTotalIncome(new java.math.BigDecimal(stats.get("income").toString()));
            } else {
                car.setRentCount(0);
                car.setTotalIncome(java.math.BigDecimal.ZERO);
            }
        }
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
