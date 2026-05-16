package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.common.PageResult;
import com.sdjzu.carrental.mapper.CarImageMapper;
import com.sdjzu.carrental.mapper.CarMapper;
import com.sdjzu.carrental.model.entity.Car;
import com.sdjzu.carrental.model.entity.CarImage;
import com.sdjzu.carrental.model.request.CarImageItemRequest;
import com.sdjzu.carrental.model.request.CarRequest;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CarService {

    private final CarMapper carMapper;
    private final CarImageMapper carImageMapper;
    private final MediaService mediaService;

    public CarService(CarMapper carMapper, CarImageMapper carImageMapper, MediaService mediaService) {
        this.carMapper = carMapper;
        this.carImageMapper = carImageMapper;
        this.mediaService = mediaService;
    }

    public List<String> listBrands() {
        return carMapper.selectList(new LambdaQueryWrapper<Car>().select(Car::getBrand).groupBy(Car::getBrand))
                .stream().map(Car::getBrand).collect(Collectors.toList());
    }

    public List<String> listCities() {
        return carMapper.selectList(new LambdaQueryWrapper<Car>()
                        .select(Car::getCity)
                        .eq(Car::getStatus, "AVAILABLE")
                        .isNotNull(Car::getCity)
                        .ne(Car::getCity, "")
                        .groupBy(Car::getCity))
                .stream()
                .map(Car::getCity)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    public PageResult<Car> list(String brand, Long typeId, String city, String status, String sort, String keyword, int pageNum, int pageSize) {
        Page<Car> page;
        if ("rentCount".equals(sort) || "totalIncome".equals(sort)) {
            String orderCol = "rentCount".equals(sort) ? "s.cnt" : "s.income";
            int offset = (pageNum - 1) * pageSize;
            long total = carMapper.selectFilteredCount(brand, typeId, city, status);
            List<Car> cars = carMapper.selectWithRentalStats(brand, typeId, city, status, orderCol, "DESC", offset, pageSize);
            page = new Page<>(pageNum, pageSize, total);
            page.setRecords(cars);
        } else {
            LambdaQueryWrapper<Car> wrapper = new LambdaQueryWrapper<Car>()
                    .like(StringUtils.hasText(brand), Car::getBrand, brand)
                    .eq(typeId != null, Car::getTypeId, typeId)
                    .eq(StringUtils.hasText(city), Car::getCity, city)
                    .eq(StringUtils.hasText(status), Car::getStatus, status);
            if (StringUtils.hasText(keyword)) {
                wrapper.and(w -> w
                        .like(Car::getBrand, keyword)
                        .or().like(Car::getModel, keyword)
                        .or().like(Car::getPlateNumber, keyword)
                );
            }
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
        attachImages(page.getRecords(), false);
        PageResult<Car> result = PageResult.of(page);
        result.summary("totalCount", carMapper.selectCount(null));
        result.summary("available", carMapper.selectCount(new LambdaQueryWrapper<Car>().eq(Car::getStatus, "AVAILABLE")));
        result.summary("reserved", carMapper.selectCount(new LambdaQueryWrapper<Car>().eq(Car::getStatus, "RESERVED")));
        result.summary("rented", carMapper.selectCount(new LambdaQueryWrapper<Car>().eq(Car::getStatus, "RENTED")));
        result.summary("awaitingRepair", carMapper.selectCount(new LambdaQueryWrapper<Car>().eq(Car::getStatus, "AWAITING_REPAIR")));
        result.summary("repairing", carMapper.selectCount(new LambdaQueryWrapper<Car>().eq(Car::getStatus, "REPAIRING")));
        result.summary("disabled", carMapper.selectCount(new LambdaQueryWrapper<Car>().eq(Car::getStatus, "DISABLED")));
        return result;
    }

    private void enrichWithRentalStats(List<Car> cars) {
        if (cars == null || cars.isEmpty()) return;
        Map<Long, Map<String, Object>> statsMap = carMapper.selectRentalStats().stream()
                .collect(Collectors.toMap(m -> ((Number) m.get("carId")).longValue(), m -> m, (a, b) -> a));
        Map<Long, String> renterMap = carMapper.selectCurrentRenters().stream()
                .collect(Collectors.toMap(m -> ((Number) m.get("carId")).longValue(), m -> (String) m.get("renterName"), (a, b) -> a));
        for (Car car : cars) {
            Map<String, Object> stats = statsMap.get(car.getId());
            if (stats != null) {
                car.setRentCount(((Number) stats.get("cnt")).intValue());
                car.setTotalIncome(new java.math.BigDecimal(stats.get("income").toString()));
            } else {
                car.setRentCount(0);
                car.setTotalIncome(java.math.BigDecimal.ZERO);
            }
            car.setCurrentRenterName(renterMap.get(car.getId()));
        }
    }

    public Car detail(Long id) {
        Car car = carMapper.selectById(id);
        if (car == null) {
            throw new BusinessException("车辆不存在");
        }
        attachImages(List.of(car), true);
        return car;
    }

    @Transactional
    public void add(CarRequest request) {
        SecurityUtils.requireAdmin();
        Car car = new Car();
        BeanUtils.copyProperties(request, car);
        car.setCarNo("TEMP");
        car.setPickupAddress(buildPickupAddress(request.getProvince(), request.getCity(), request.getDetailAddress()));
        car.setCarImage(null);
        car.setStatus("AVAILABLE");
        carMapper.insert(car);
        car.setCarNo(generateCarNo(car.getId()));
        carMapper.updateById(car);
        saveCarImages(car.getId(), car.getCarNo(), request.getImages());
    }

    @Transactional
    public void update(Long id, CarRequest request) {
        SecurityUtils.requireAdmin();
        Car existing = carMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("车辆不存在");
        }
        Car car = new Car();
        BeanUtils.copyProperties(request, car);
        car.setId(id);
        car.setCarNo(existing.getCarNo());
        car.setPickupAddress(buildPickupAddress(request.getProvince(), request.getCity(), request.getDetailAddress()));
        car.setCarImage(existing.getCarImage());
        car.setStatus(existing.getStatus());
        carMapper.updateById(car);
        saveCarImages(id, existing.getCarNo(), request.getImages());
    }

    @Transactional
    public void delete(Long id) {
        SecurityUtils.requireAdmin();
        Car car = carMapper.selectById(id);
        if (car == null) {
            throw new BusinessException("车辆不存在");
        }
        if (!"AVAILABLE".equals(car.getStatus()) && !"DISABLED".equals(car.getStatus())) {
            throw new BusinessException("只能删除空闲或停用状态的车辆，当前状态: " + car.getStatus());
        }
        List<CarImage> existingImages = carImageMapper.selectList(new LambdaQueryWrapper<CarImage>().eq(CarImage::getCarId, id));
        carImageMapper.delete(new LambdaQueryWrapper<CarImage>().eq(CarImage::getCarId, id));
        carMapper.deleteById(id);
        deleteOrphanManagedImages(
                existingImages.stream()
                        .map(CarImage::getImageUrl)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toSet()),
                id
        );
        mediaService.deleteCarImageFolder(car.getCarNo());
    }

    public void disable(Long id) {
        SecurityUtils.requireAdmin();
        Car car = carMapper.selectById(id);
        if (car == null) {
            throw new BusinessException("车辆不存在");
        }
        if ("RENTED".equals(car.getStatus()) || "RESERVED".equals(car.getStatus())) {
            throw new BusinessException("车辆正在租赁中，无法停用");
        }
        car.setStatus("DISABLED");
        carMapper.updateById(car);
    }

    public void enable(Long id) {
        SecurityUtils.requireAdmin();
        Car car = carMapper.selectById(id);
        if (car == null) {
            throw new BusinessException("车辆不存在");
        }
        if (!"DISABLED".equals(car.getStatus())) {
            throw new BusinessException("只有停用状态的车辆才能启用");
        }
        car.setStatus("AVAILABLE");
        carMapper.updateById(car);
    }

    public List<Car> enrichCarsForDisplay(List<Car> cars, boolean includeImages) {
        attachImages(cars, includeImages);
        return cars;
    }

    private void attachImages(List<Car> cars, boolean includeImages) {
        if (cars == null || cars.isEmpty()) return;
        List<Long> carIds = cars.stream().map(Car::getId).toList();
        List<CarImage> images = carImageMapper.selectList(new LambdaQueryWrapper<CarImage>()
                .in(CarImage::getCarId, carIds)
                .orderByAsc(CarImage::getSortOrder)
                .orderByAsc(CarImage::getId));
        Map<Long, List<CarImage>> imageMap = images.stream().collect(Collectors.groupingBy(CarImage::getCarId));
        for (Car car : cars) {
            List<CarImage> carImages = new ArrayList<>(imageMap.getOrDefault(car.getId(), List.of()));
            if (!carImages.isEmpty()) {
                car.setCarImage(carImages.get(0).getImageUrl());
            }
            if (includeImages) {
                car.setCarImages(carImages);
            }
        }
    }

    private void saveCarImages(Long carId, String carNo, List<CarImageItemRequest> images) {
        List<CarImage> existingImages = carImageMapper.selectList(new LambdaQueryWrapper<CarImage>().eq(CarImage::getCarId, carId));
        List<CarImageItemRequest> sourceImages = images == null ? List.of() : images;
        List<CarImageItemRequest> sortedImages = sourceImages.stream()
                .sorted(Comparator.comparing(CarImageItemRequest::getSortOrder))
                .toList();
        List<String> normalizedUrls = sortedImages.stream()
                .map(item -> mediaService.moveCarImageToCarFolder(item.getImageUrl(), carNo))
                .toList();
        Set<String> retainedUrls = normalizedUrls.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Set<String> removedUrls = existingImages.stream()
                .map(CarImage::getImageUrl)
                .filter(StringUtils::hasText)
                .filter(url -> !retainedUrls.contains(url))
                .collect(Collectors.toSet());
        carImageMapper.delete(new LambdaQueryWrapper<CarImage>().eq(CarImage::getCarId, carId));
        String mediaPrefix = mediaService.getMediaAccessPrefix() + "/";
        for (int i = 0; i < sortedImages.size(); i++) {
            CarImageItemRequest item = sortedImages.get(i);
            String normalizedUrl = normalizedUrls.get(i);
            CarImage image = new CarImage();
            image.setCarId(carId);
            image.setImageUrl(normalizedUrl);
            image.setSortOrder(item.getSortOrder());
            image.setSourceType(StringUtils.hasText(normalizedUrl) && normalizedUrl.startsWith(mediaPrefix) ? "SERVER" : "URL_IMPORT");
            image.setOriginUrl(item.getImageUrl());
            carImageMapper.insert(image);
        }
        Car update = new Car();
        update.setId(carId);
        update.setCarImage(normalizedUrls.isEmpty() ? null : normalizedUrls.get(0));
        carMapper.updateById(update);
        deleteOrphanManagedImages(removedUrls, carId);
    }

    private void deleteOrphanManagedImages(Set<String> imageUrls, Long currentCarId) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        for (String imageUrl : imageUrls) {
            long referencedCount = carImageMapper.selectCount(new LambdaQueryWrapper<CarImage>()
                    .eq(CarImage::getImageUrl, imageUrl)
                    .ne(currentCarId != null, CarImage::getCarId, currentCarId));
            if (referencedCount == 0) {
                mediaService.deleteCarImageIfManaged(imageUrl);
            }
        }
    }

    private String buildPickupAddress(String province, String city, String detailAddress) {
        return province + city + detailAddress;
    }

    private String generateCarNo(Long id) {
        return String.format("CAR%06d", id);
    }
}
