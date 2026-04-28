package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdjzu.carrental.mapper.CarMapper;
import com.sdjzu.carrental.mapper.FaultReportMapper;
import com.sdjzu.carrental.mapper.RentOrderMapper;
import com.sdjzu.carrental.mapper.ReturnOrderMapper;
import com.sdjzu.carrental.mapper.UserMapper;
import com.sdjzu.carrental.model.entity.Car;
import com.sdjzu.carrental.model.entity.FaultReport;
import com.sdjzu.carrental.model.entity.RentOrder;
import com.sdjzu.carrental.model.entity.ReturnOrder;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final UserMapper userMapper;
    private final CarMapper carMapper;
    private final RentOrderMapper rentOrderMapper;
    private final ReturnOrderMapper returnOrderMapper;
    private final FaultReportMapper faultReportMapper;

    public DashboardService(UserMapper userMapper,
                            CarMapper carMapper,
                            RentOrderMapper rentOrderMapper,
                            ReturnOrderMapper returnOrderMapper,
                            FaultReportMapper faultReportMapper) {
        this.userMapper = userMapper;
        this.carMapper = carMapper;
        this.rentOrderMapper = rentOrderMapper;
        this.returnOrderMapper = returnOrderMapper;
        this.faultReportMapper = faultReportMapper;
    }

    public Map<String, Object> overview() {
        SecurityUtils.requireAdmin();
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userMapper.selectCount(null));
        data.put("carCount", carMapper.selectCount(null));
        data.put("availableCarCount", carMapper.selectCount(new LambdaQueryWrapper<Car>().eq(Car::getStatus, "AVAILABLE")));
        data.put("rentedCarCount", carMapper.selectCount(new LambdaQueryWrapper<Car>().eq(Car::getStatus, "RENTED")));
        data.put("maintenanceCarCount", carMapper.selectCount(new LambdaQueryWrapper<Car>().eq(Car::getStatus, "MAINTENANCE")));
        data.put("rentOrderCount", rentOrderMapper.selectCount(null));
        data.put("returnOrderCount", returnOrderMapper.selectCount(null));
        data.put("faultReportCount", faultReportMapper.selectCount(null));
        data.put("pendingFaultCount", faultReportMapper.selectCount(new LambdaQueryWrapper<FaultReport>().eq(FaultReport::getFaultStatus, "PENDING")));
        data.put("activeRentCount", rentOrderMapper.selectCount(new LambdaQueryWrapper<RentOrder>().eq(RentOrder::getOrderStatus, "RENTED")));
        data.put("confirmedReturnCount", returnOrderMapper.selectCount(new LambdaQueryWrapper<ReturnOrder>().eq(ReturnOrder::getStatus, "CONFIRMED")));

        BigDecimal rentIncome = BigDecimal.ZERO;
        for (RentOrder order : rentOrderMapper.selectList(null)) {
            if (order.getTotalPrice() != null) {
                rentIncome = rentIncome.add(order.getTotalPrice());
            }
        }
        BigDecimal extraIncome = BigDecimal.ZERO;
        for (ReturnOrder order : returnOrderMapper.selectList(null)) {
            if (order.getExtraFee() != null) {
                extraIncome = extraIncome.add(order.getExtraFee());
            }
        }
        BigDecimal totalIncome = rentIncome.add(extraIncome);
        data.put("rentIncome", rentIncome);
        data.put("extraIncome", extraIncome);
        data.put("totalIncome", totalIncome);
        return data;
    }

    public Map<String, Object> charts(String period, String range) {
        SecurityUtils.requireAdmin();
        String normalized = period == null ? "day" : period.toLowerCase();
        boolean isMonth = "month".equals(normalized);
        boolean isYear = "year".equals(normalized);

        String normalizedRange = range == null ? "" : range.toLowerCase();
        LocalDate today = LocalDate.now();

        LocalDate startDate;
        LocalDate endDate = today;

        if (isYear) {
            int years = 5;
            if ("3y".equals(normalizedRange)) {
                years = 3;
            } else if ("5y".equals(normalizedRange)) {
                years = 5;
            } else if ("all".equals(normalizedRange)) {
                years = 0;
            }
            if (years == 0) {
                startDate = null;
            } else {
                startDate = today.minusYears(years - 1L).withDayOfYear(1);
            }
        } else if (isMonth) {
            int months = 6;
            if ("6m".equals(normalizedRange)) {
                months = 6;
            } else if ("12m".equals(normalizedRange)) {
                months = 12;
            } else if ("all".equals(normalizedRange)) {
                months = 0;
            }
            if (months == 0) {
                startDate = null;
            } else {
                YearMonth startMonth = YearMonth.from(today).minusMonths(months - 1L);
                startDate = startMonth.atDay(1);
            }
        } else {
            int days = 7;
            if ("7d".equals(normalizedRange)) {
                days = 7;
            } else if ("30d".equals(normalizedRange)) {
                days = 30;
            } else if ("90d".equals(normalizedRange)) {
                days = 90;
            } else if ("all".equals(normalizedRange)) {
                days = 0;
            }
            if (days == 0) {
                startDate = null;
            } else {
                startDate = today.minusDays(days - 1L);
            }
        }

        List<RentOrder> rentOrders = rentOrderMapper.selectList(new LambdaQueryWrapper<RentOrder>());
        Map<String, Long> rentCountByKey = new HashMap<>();
        Map<String, BigDecimal> rentIncomeByKey = new HashMap<>();
        LocalDate minDate = null;
        LocalDate maxDate = null;
        for (RentOrder rentOrder : rentOrders) {
            LocalDateTime createdTime = rentOrder.getCreateTime();
            if (createdTime == null) {
                continue;
            }
            LocalDate createdDate = createdTime.toLocalDate();
            if (startDate != null && (createdDate.isBefore(startDate) || createdDate.isAfter(endDate))) {
                continue;
            }
            if (minDate == null || createdDate.isBefore(minDate)) {
                minDate = createdDate;
            }
            if (maxDate == null || createdDate.isAfter(maxDate)) {
                maxDate = createdDate;
            }
            String key;
            if (isYear) {
                key = String.valueOf(createdDate.getYear());
            } else if (isMonth) {
                key = createdDate.getYear() + "-" + String.format("%02d", createdDate.getMonthValue());
            } else {
                key = createdDate.toString();
            }
            rentCountByKey.put(key, rentCountByKey.getOrDefault(key, 0L) + 1L);
            if (rentOrder.getTotalPrice() != null) {
                BigDecimal current = rentIncomeByKey.getOrDefault(key, BigDecimal.ZERO);
                rentIncomeByKey.put(key, current.add(rentOrder.getTotalPrice()));
            }
        }

        List<ReturnOrder> returnOrders = returnOrderMapper.selectList(new LambdaQueryWrapper<ReturnOrder>());
        Map<String, Long> returnCountByKey = new HashMap<>();
        Map<String, BigDecimal> extraIncomeByKey = new HashMap<>();
        for (ReturnOrder returnOrder : returnOrders) {
            LocalDateTime returnTime = returnOrder.getActualReturnTime();
            if (returnTime == null) {
                continue;
            }
            LocalDate returnDate = returnTime.toLocalDate();
            if (startDate != null && (returnDate.isBefore(startDate) || returnDate.isAfter(endDate))) {
                continue;
            }
            if (minDate == null || returnDate.isBefore(minDate)) {
                minDate = returnDate;
            }
            if (maxDate == null || returnDate.isAfter(maxDate)) {
                maxDate = returnDate;
            }
            String key;
            if (isYear) {
                key = String.valueOf(returnDate.getYear());
            } else if (isMonth) {
                key = returnDate.getYear() + "-" + String.format("%02d", returnDate.getMonthValue());
            } else {
                key = returnDate.toString();
            }
            returnCountByKey.put(key, returnCountByKey.getOrDefault(key, 0L) + 1L);
            if (returnOrder.getExtraFee() != null) {
                BigDecimal current = extraIncomeByKey.getOrDefault(key, BigDecimal.ZERO);
                extraIncomeByKey.put(key, current.add(returnOrder.getExtraFee()));
            }
        }

        if (startDate != null) {
            minDate = startDate;
            maxDate = endDate;
        } else if (minDate == null || maxDate == null) {
            LocalDate fallbackEnd = LocalDate.now();
            LocalDate fallbackStart = fallbackEnd.minusDays(6);
            minDate = fallbackStart;
            maxDate = fallbackEnd;
        }

        List<String> keys = new ArrayList<>();
        if (isYear) {
            Year startYear = Year.of(minDate.getYear());
            Year endYear = Year.of(maxDate.getYear());
            Year cursor = startYear;
            while (!cursor.isAfter(endYear)) {
                keys.add(cursor.toString());
                cursor = cursor.plusYears(1);
            }
        } else if (isMonth) {
            YearMonth startMonth = YearMonth.of(minDate.getYear(), minDate.getMonthValue());
            YearMonth endMonth = YearMonth.of(maxDate.getYear(), maxDate.getMonthValue());
            YearMonth cursor = startMonth;
            while (!cursor.isAfter(endMonth)) {
                keys.add(cursor.getYear() + "-" + String.format("%02d", cursor.getMonthValue()));
                cursor = cursor.plusMonths(1);
            }
        } else {
            LocalDate cursor = minDate;
            while (!cursor.isAfter(maxDate)) {
                keys.add(cursor.toString());
                cursor = cursor.plusDays(1);
            }
        }

        List<String> dates = new ArrayList<>(keys);
        List<Long> rentCounts = new ArrayList<>();
        List<Long> returnCounts = new ArrayList<>();
        List<BigDecimal> rentIncomeSeries = new ArrayList<>();
        List<BigDecimal> extraIncomeSeries = new ArrayList<>();
        List<BigDecimal> totalIncomeSeries = new ArrayList<>();
        for (String key : keys) {
            rentCounts.add(rentCountByKey.getOrDefault(key, 0L));
            returnCounts.add(returnCountByKey.getOrDefault(key, 0L));
            BigDecimal rentIncome = rentIncomeByKey.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal extraIncome = extraIncomeByKey.getOrDefault(key, BigDecimal.ZERO);
            rentIncomeSeries.add(rentIncome);
            extraIncomeSeries.add(extraIncome);
            totalIncomeSeries.add(rentIncome.add(extraIncome));
        }

        long availableCount = carMapper.selectCount(new LambdaQueryWrapper<Car>().eq(Car::getStatus, "AVAILABLE"));
        long rentedCount = carMapper.selectCount(new LambdaQueryWrapper<Car>().eq(Car::getStatus, "RENTED"));
        long maintenanceCount = carMapper.selectCount(new LambdaQueryWrapper<Car>().eq(Car::getStatus, "MAINTENANCE"));

        List<Map<String, Object>> carStatus = new ArrayList<>();
        Map<String, Object> available = new HashMap<>();
        available.put("name", "AVAILABLE");
        available.put("value", availableCount);
        carStatus.add(available);

        Map<String, Object> rented = new HashMap<>();
        rented.put("name", "RENTED");
        rented.put("value", rentedCount);
        carStatus.add(rented);

        Map<String, Object> maintenance = new HashMap<>();
        maintenance.put("name", "MAINTENANCE");
        maintenance.put("value", maintenanceCount);
        carStatus.add(maintenance);

        Map<String, Object> data = new HashMap<>();
        data.put("dates", dates);
        data.put("rentCounts", rentCounts);
        data.put("returnCounts", returnCounts);
        data.put("rentIncomeSeries", rentIncomeSeries);
        data.put("extraIncomeSeries", extraIncomeSeries);
        data.put("totalIncomeSeries", totalIncomeSeries);
        data.put("carStatus", carStatus);
        return data;
    }
}
