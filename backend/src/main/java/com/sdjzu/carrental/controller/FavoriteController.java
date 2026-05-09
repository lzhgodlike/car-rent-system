package com.sdjzu.carrental.controller;

import com.sdjzu.carrental.common.ApiResponse;
import com.sdjzu.carrental.model.entity.Car;
import com.sdjzu.carrental.service.FavoriteService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public ApiResponse<List<Car>> list() {
        return ApiResponse.success(favoriteService.list());
    }

    @PostMapping("/{carId}")
    public ApiResponse<Void> add(@PathVariable Long carId) {
        favoriteService.add(carId);
        return ApiResponse.success("收藏成功", null);
    }

    @DeleteMapping("/{carId}")
    public ApiResponse<Void> remove(@PathVariable Long carId) {
        favoriteService.remove(carId);
        return ApiResponse.success("已取消收藏", null);
    }

    @GetMapping("/{carId}/check")
    public ApiResponse<Boolean> check(@PathVariable Long carId) {
        return ApiResponse.success(favoriteService.isFavorite(carId));
    }
}
