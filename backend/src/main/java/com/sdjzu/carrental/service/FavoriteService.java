package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.mapper.CarMapper;
import com.sdjzu.carrental.mapper.FavoriteMapper;
import com.sdjzu.carrental.model.entity.Car;
import com.sdjzu.carrental.model.entity.Favorite;
import com.sdjzu.carrental.model.dto.LoginUser;
import com.sdjzu.carrental.common.UserContext;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final CarMapper carMapper;
    private final CarService carService;

    public FavoriteService(FavoriteMapper favoriteMapper, CarMapper carMapper, CarService carService) {
        this.favoriteMapper = favoriteMapper;
        this.carMapper = carMapper;
        this.carService = carService;
    }

    public void add(Long carId) {
        LoginUser user = UserContext.get();
        if (user == null) {
            throw new BusinessException("请先登录");
        }
        Long userId = user.getUserId();
        // 检查是否已收藏
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getCarId, carId)
        );
        if (count > 0) {
            throw new BusinessException("已收藏该车辆");
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setCarId(carId);
        favoriteMapper.insert(favorite);
    }

    public void remove(Long carId) {
        LoginUser user = UserContext.get();
        if (user == null) {
            throw new BusinessException("请先登录");
        }
        Long userId = user.getUserId();
        favoriteMapper.delete(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getCarId, carId)
        );
    }

    public List<Car> list() {
        LoginUser user = UserContext.get();
        if (user == null) {
            return Collections.emptyList();
        }
        Long userId = user.getUserId();
        List<Long> carIds = favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime)
        ).stream().map(Favorite::getCarId).collect(Collectors.toList());
        if (carIds.isEmpty()) {
            return Collections.emptyList();
        }
        return carService.enrichCarsForDisplay(carMapper.selectBatchIds(carIds), false);
    }

    public boolean isFavorite(Long carId) {
        LoginUser user = UserContext.get();
        if (user == null) {
            return false;
        }
        Long userId = user.getUserId();
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getCarId, carId)
        );
        return count > 0;
    }
}
