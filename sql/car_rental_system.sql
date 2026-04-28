/*
 Navicat Premium Data Transfer

 Source Server         : mysql
 Source Server Type    : MySQL
 Source Server Version : 80022
 Source Host           : localhost:3306
 Source Schema         : car_rental_system

 Target Server Type    : MySQL
 Target Server Version : 80022
 File Encoding         : 65001

 Date: 23/03/2026 15:24:09
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for car_info
-- ----------------------------
DROP TABLE IF EXISTS `car_info`;
CREATE TABLE `car_info`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `car_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `type_id` bigint(0) NOT NULL,
  `brand` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `plate_number` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `day_price` decimal(10, 2) NOT NULL,
  `mileage` int(0) NOT NULL DEFAULT 0,
  `pickup_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `car_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'AVAILABLE',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `car_no`(`car_no`) USING BTREE,
  UNIQUE INDEX `plate_number`(`plate_number`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of car_info
-- ----------------------------
INSERT INTO `car_info` VALUES (1, 'CAR001', 1, '大众', '朗逸 2024', '鲁A12345', 180.00, 12000, '山东建筑大学长清校区东门', 'https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=800&q=80', 'AVAILABLE', '2026-03-08 23:12:23', '2026-03-08 23:12:23');
INSERT INTO `car_info` VALUES (2, 'CAR002', 2, '哈弗', 'H6 2023', '鲁A23456', 260.00, 12, '济南西站停车场', 'https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=800&q=80', 'AVAILABLE', '2026-03-08 23:12:23', '2026-03-09 11:49:19');
INSERT INTO `car_info` VALUES (3, 'CAR003', 3, '比亚迪', '秦PLUS EV', '鲁A34567', 220.00, 18000, '泉城广场地下一层', 'https://images.unsplash.com/photo-1553440569-bcc63803a83d?auto=format&fit=crop&w=800&q=80', 'AVAILABLE', '2026-03-08 23:12:23', '2026-03-09 11:49:14');

-- ----------------------------
-- Table structure for car_type
-- ----------------------------
DROP TABLE IF EXISTS `car_type`;
CREATE TABLE `car_type`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `type_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of car_type
-- ----------------------------
INSERT INTO `car_type` VALUES (1, '轿车', '适合城市通勤与家庭出行', '2026-03-08 23:12:23', '2026-03-08 23:12:23');
INSERT INTO `car_type` VALUES (2, 'SUV', '适合长途出行与复杂路况', '2026-03-08 23:12:23', '2026-03-08 23:12:23');
INSERT INTO `car_type` VALUES (3, '新能源', '节能环保，适合短中途使用', '2026-03-08 23:12:23', '2026-03-08 23:12:23');

-- ----------------------------
-- Table structure for fault_report
-- ----------------------------
DROP TABLE IF EXISTS `fault_report`;
CREATE TABLE `fault_report`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `car_id` bigint(0) NOT NULL,
  `fault_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `fault_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING',
  `handle_result` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `report_time` datetime(0) NOT NULL,
  `handle_time` datetime(0) NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of fault_report
-- ----------------------------
INSERT INTO `fault_report` VALUES (1, 2, 3, '车辆电池提示异常，请安排检修', 'RESOLVED', '已修复', '2026-03-08 09:30:00', '2026-03-09 11:44:42', '2026-03-08 23:12:23', '2026-03-08 23:12:23');
INSERT INTO `fault_report` VALUES (2, 3, 2, '没油了', 'RESOLVED', '好了', '2026-03-09 11:39:38', '2026-03-09 11:44:45', '2026-03-09 11:39:38', '2026-03-09 11:39:38');
INSERT INTO `fault_report` VALUES (3, 3, 1, '没电了', 'RESOLVED', '已修复', '2026-03-09 11:50:20', '2026-03-09 11:50:46', '2026-03-09 11:50:20', '2026-03-09 11:50:20');
INSERT INTO `fault_report` VALUES (4, 3, 1, '没电了', 'RESOLVED', '修复中', '2026-03-09 11:51:19', '2026-03-09 11:51:30', '2026-03-09 11:51:18', '2026-03-09 11:51:18');
INSERT INTO `fault_report` VALUES (5, 3, 3, '没电了', 'RESOLVED', '修复完成', '2026-03-09 11:53:51', '2026-03-09 11:58:30', '2026-03-09 11:53:50', '2026-03-09 11:53:50');
INSERT INTO `fault_report` VALUES (6, 3, 3, '没电了', 'RESOLVED', '已维修', '2026-03-12 11:46:42', '2026-03-12 11:47:51', '2026-03-12 11:46:41', '2026-03-12 11:46:41');
INSERT INTO `fault_report` VALUES (7, 3, 1, '后视镜坏了', 'RESOLVED', '已安排维修', '2026-03-23 14:28:12', '2026-03-23 14:28:29', '2026-03-23 14:28:12', '2026-03-23 14:28:12');
INSERT INTO `fault_report` VALUES (8, 2, 3, 'Battery warning', 'RESOLVED', '已安排维修', '2026-03-08 09:30:00', '2026-03-23 15:13:52', '2026-03-08 09:30:00', '2026-03-08 09:30:00');
INSERT INTO `fault_report` VALUES (9, 3, 1, 'Brake noise', 'RESOLVED', 'Scheduled maintenance', '2026-03-10 13:20:00', '2026-03-23 15:13:53', '2026-03-10 13:20:00', '2026-03-10 15:00:00');

-- ----------------------------
-- Table structure for rent_order
-- ----------------------------
DROP TABLE IF EXISTS `rent_order`;
CREATE TABLE `rent_order`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` bigint(0) NOT NULL,
  `car_id` bigint(0) NOT NULL,
  `rent_date` date NOT NULL,
  `expected_return_date` date NOT NULL,
  `actual_return_date` date NULL DEFAULT NULL,
  `rent_days` int(0) NOT NULL,
  `unit_price` decimal(10, 2) NOT NULL,
  `total_price` decimal(10, 2) NOT NULL,
  `order_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'RENTED',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_no`(`order_no`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rent_order
-- ----------------------------
INSERT INTO `rent_order` VALUES (2, 'RENT1773026775825', 3, 2, '2026-03-09', '2026-03-11', '2026-03-09', 2, 260.00, 520.00, 'RETURNED', '', '2026-03-09 11:26:15', '2026-03-09 11:26:15');
INSERT INTO `rent_order` VALUES (3, 'RENT1773026865687', 3, 2, '2026-03-09', '2026-03-11', '2026-03-09', 2, 260.00, 520.00, 'RETURNED', '', '2026-03-09 11:27:45', '2026-03-09 11:27:45');
INSERT INTO `rent_order` VALUES (4, 'RENT1773028182194', 3, 2, '2026-03-09', '2026-03-12', '2026-03-12', 3, 260.00, 780.00, 'RETURNED', '', '2026-03-09 11:49:42', '2026-03-09 11:49:42');
INSERT INTO `rent_order` VALUES (5, 'RENT1773286879685', 3, 2, '2026-03-13', '2026-03-15', '2026-03-12', 2, 260.00, 520.00, 'RETURNED', '', '2026-03-12 11:41:19', '2026-03-12 11:41:19');
INSERT INTO `rent_order` VALUES (6, 'RENT1774233747990', 3, 2, '2026-03-23', '2026-03-25', '2026-03-23', 2, 260.00, 520.00, 'RETURNED', '', '2026-03-21 10:42:27', '2026-03-23 14:35:37');
INSERT INTO `rent_order` VALUES (7, 'RENT1774233803444', 3, 2, '2026-03-23', '2026-03-25', '2026-03-23', 2, 260.00, 520.00, 'RETURNED', '', '2026-03-22 10:43:23', '2026-03-23 14:35:42');
INSERT INTO `rent_order` VALUES (8, 'RENT1774247034828', 3, 1, '2026-03-24', '2026-03-26', '2026-03-23', 2, 180.00, 360.00, 'RETURNED', '', '2026-03-23 14:23:54', '2026-03-23 14:23:54');
INSERT INTO `rent_order` VALUES (9, 'RENT202603080001', 2, 2, '2026-03-06', '2026-03-10', NULL, 4, 260.00, 1040.00, 'RENTED', 'Weekend drive', '2026-03-08 10:10:00', '2026-03-08 10:10:00');
INSERT INTO `rent_order` VALUES (10, 'RENT202603170001', 2, 1, '2026-03-17', '2026-03-20', NULL, 3, 180.00, 540.00, 'RENTED', 'Spring trip', '2026-03-17 09:12:00', '2026-03-17 09:12:00');
INSERT INTO `rent_order` VALUES (11, 'RENT202603160001', 3, 2, '2026-03-16', '2026-03-18', '2026-03-18', 2, 260.00, 520.00, 'RETURNED', 'Business', '2026-03-16 08:20:00', '2026-03-18 18:30:00');
INSERT INTO `rent_order` VALUES (12, 'RENT202603150001', 2, 3, '2026-03-15', '2026-03-19', '2026-03-19', 4, 220.00, 880.00, 'RETURNED', 'Family visit', '2026-03-15 10:05:00', '2026-03-19 14:10:00');
INSERT INTO `rent_order` VALUES (13, 'RENT202603090001', 3, 1, '2026-03-09', '2026-03-11', '2026-03-11', 2, 180.00, 360.00, 'RETURNED', 'Short trip', '2026-03-09 11:00:00', '2026-03-11 10:00:00');
INSERT INTO `rent_order` VALUES (14, 'RENT202602120001', 2, 2, '2026-02-12', '2026-02-15', '2026-02-15', 3, 260.00, 780.00, 'RETURNED', 'Weekend', '2026-02-12 09:00:00', '2026-02-15 17:20:00');
INSERT INTO `rent_order` VALUES (15, 'RENT202601050001', 3, 3, '2026-01-05', '2026-01-07', '2026-01-07', 2, 220.00, 440.00, 'RETURNED', 'City run', '2026-01-05 08:10:00', '2026-01-07 12:10:00');
INSERT INTO `rent_order` VALUES (16, 'RENT202512200001', 2, 1, '2025-12-20', '2025-12-22', '2025-12-22', 2, 180.00, 360.00, 'RETURNED', 'Holiday', '2025-12-20 10:30:00', '2025-12-22 19:30:00');
INSERT INTO `rent_order` VALUES (17, 'RENT202407080001', 3, 2, '2024-07-08', '2024-07-10', '2024-07-10', 2, 260.00, 520.00, 'RETURNED', 'Summer', '2024-07-08 09:40:00', '2024-07-10 16:00:00');
INSERT INTO `rent_order` VALUES (18, 'RENT202303150001', 2, 3, '2023-03-15', '2023-03-18', '2023-03-18', 3, 220.00, 660.00, 'RETURNED', 'Old data', '2023-03-15 13:20:00', '2023-03-18 12:20:00');
INSERT INTO `rent_order` VALUES (19, 'RENT202201120001', 3, 1, '2022-01-12', '2022-01-14', '2022-01-14', 2, 180.00, 360.00, 'RETURNED', 'Legacy', '2022-01-12 08:00:00', '2022-01-14 10:00:00');

-- ----------------------------
-- Table structure for return_order
-- ----------------------------
DROP TABLE IF EXISTS `return_order`;
CREATE TABLE `return_order`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `rent_order_id` bigint(0) NOT NULL,
  `actual_return_time` datetime(0) NOT NULL,
  `actual_mileage` int(0) NOT NULL,
  `damage_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `extra_fee` decimal(10, 2) NULL DEFAULT 0.00,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING',
  `operator_id` bigint(0) NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `rent_order_id`(`rent_order_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of return_order
-- ----------------------------
INSERT INTO `return_order` VALUES (1, 2, '2026-03-09 11:26:49', 2, '', 0.00, 'CONFIRMED', 1, '2026-03-09 11:26:48', '2026-03-09 11:26:48');
INSERT INTO `return_order` VALUES (2, 3, '2026-03-09 11:38:28', 500, '', 0.00, 'CONFIRMED', 1, '2026-03-09 11:38:27', '2026-03-09 11:38:27');
INSERT INTO `return_order` VALUES (3, 4, '2026-03-09 11:50:11', 2, '没损坏', 0.00, 'CONFIRMED', 1, '2026-03-09 11:50:11', '2026-03-09 11:50:11');
INSERT INTO `return_order` VALUES (4, 5, '2026-03-14 11:42:44', 8, '', 0.00, 'CONFIRMED', 1, '2026-03-14 11:42:44', '2026-03-23 14:36:37');
INSERT INTO `return_order` VALUES (5, 6, '2026-03-23 10:42:45', 2, '', 0.00, 'CONFIRMED', 1, '2026-03-23 10:42:44', '2026-03-23 10:42:44');
INSERT INTO `return_order` VALUES (6, 8, '2026-03-23 14:24:04', 0, '', 0.00, 'CONFIRMED', 1, '2026-03-23 14:24:03', '2026-03-23 14:24:03');
INSERT INTO `return_order` VALUES (7, 7, '2026-03-23 14:24:32', 0, '', 0.00, 'CONFIRMED', 1, '2026-03-23 14:24:32', '2026-03-23 14:24:32');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `id_card` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `gender` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'USER',
  `status` tinyint(0) NOT NULL DEFAULT 1,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '21232f297a57a5a743894a0e4a801fc3', '系统管理员', '13800000001', '370101200001010000', '女', 'ADMIN', 1, '2026-03-08 23:12:23', '2026-03-08 23:12:23');
INSERT INTO `sys_user` VALUES (2, 'zhangsan', 'e10adc3949ba59abbe56e057f20f883e', '张三', '13900000001', '370101200101010002', '男', 'USER', 1, '2026-03-08 23:12:23', '2026-03-08 23:12:23');
INSERT INTO `sys_user` VALUES (3, 'lisi', 'e10adc3949ba59abbe56e057f20f883e', '李四', '13900000002', '370101200102020003', '女', 'USER', 1, '2026-03-08 23:12:23', '2026-03-08 23:12:23');

SET FOREIGN_KEY_CHECKS = 1;
