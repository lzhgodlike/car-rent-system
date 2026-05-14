package com.sdjzu.carrental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdjzu.carrental.model.entity.Car;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CarMapper extends BaseMapper<Car> {

    @Select("SELECT ro.car_id AS carId, COUNT(*) AS cnt, " +
            "IFNULL(SUM(ro.total_price),0) + IFNULL(SUM(r.extra_fee),0) AS income " +
            "FROM rent_order ro LEFT JOIN return_order r ON ro.id = r.rent_order_id GROUP BY ro.car_id")
    List<Map<String, Object>> selectRentalStats();

    @Select("<script>" +
            "SELECT c.*, IFNULL(s.cnt,0) AS rentCount, IFNULL(s.income,0) AS totalIncome, " +
            "MAX(CASE WHEN ro2.order_status IN ('RENTED','RETURN_PENDING') THEN su.real_name END) AS currentRenterName " +
            "FROM car_info c LEFT JOIN (" +
            "SELECT ro.car_id, COUNT(*) AS cnt, IFNULL(SUM(ro.total_price),0) + IFNULL(SUM(r.extra_fee),0) AS income " +
            "FROM rent_order ro LEFT JOIN return_order r ON ro.id = r.rent_order_id GROUP BY ro.car_id" +
            ") s ON c.id=s.car_id " +
            "LEFT JOIN rent_order ro2 ON ro2.car_id = c.id " +
            "LEFT JOIN sys_user su ON su.id = ro2.user_id " +
            "<where>" +
            "<if test='brand != null and brand != \"\"'> c.brand LIKE CONCAT('%',#{brand},'%') </if>" +
            "<if test='typeId != null'> AND c.type_id = #{typeId} </if>" +
            "<if test='city != null and city != \"\"'> AND c.city = #{city} </if>" +
            "<if test='status != null and status != \"\"'> AND c.status = #{status} </if>" +
            "</where>" +
            "GROUP BY c.id " +
            "ORDER BY ${orderCol} ${orderDir} " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<Car> selectWithRentalStats(@Param("brand") String brand,
                                     @Param("typeId") Long typeId,
                                     @Param("city") String city,
                                     @Param("status") String status,
                                     @Param("orderCol") String orderCol,
                                     @Param("orderDir") String orderDir,
                                     @Param("offset") int offset,
                                     @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM car_info c " +
            "<where>" +
            "<if test='brand != null and brand != \"\"'> c.brand LIKE CONCAT('%',#{brand},'%') </if>" +
            "<if test='typeId != null'> AND c.type_id = #{typeId} </if>" +
            "<if test='city != null and city != \"\"'> AND c.city = #{city} </if>" +
            "<if test='status != null and status != \"\"'> AND c.status = #{status} </if>" +
            "</where>" +
            "</script>")
    long selectFilteredCount(@Param("brand") String brand,
                              @Param("typeId") Long typeId,
                              @Param("city") String city,
                              @Param("status") String status);

    @Select("SELECT ro.car_id AS carId, su.username AS renterName " +
            "FROM rent_order ro INNER JOIN sys_user su ON su.id = ro.user_id " +
            "WHERE ro.order_status IN ('RENTED', 'RETURN_PENDING')")
    List<Map<String, Object>> selectCurrentRenters();
}
