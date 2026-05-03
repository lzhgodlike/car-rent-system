package com.sdjzu.carrental.controller;

import com.sdjzu.carrental.common.ApiResponse;
import com.sdjzu.carrental.common.PageResult;
import com.sdjzu.carrental.model.entity.Car;
import com.sdjzu.carrental.model.request.CarRequest;
import com.sdjzu.carrental.service.CarService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @GetMapping
    public ApiResponse<PageResult<Car>> list(@RequestParam(required = false) String brand,
                                              @RequestParam(required = false) Long typeId,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String sort,
                                              @RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(carService.list(brand, typeId, status, sort, pageNum, pageSize));
    }

    @GetMapping("/brands")
    public ApiResponse<List<String>> brands() {
        return ApiResponse.success(carService.listBrands());
    }

    @GetMapping("/{id}")
    public ApiResponse<Car> detail(@PathVariable Long id) {
        return ApiResponse.success(carService.detail(id));
    }

    @PostMapping
    public ApiResponse<Void> add(@Valid @RequestBody CarRequest request) {
        carService.add(request);
        return ApiResponse.success("新增成功", null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody CarRequest request) {
        carService.update(id, request);
        return ApiResponse.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        carService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
