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

 Date: 22/06/2026 20:33:30
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for car_image
-- ----------------------------
DROP TABLE IF EXISTS `car_image`;
CREATE TABLE `car_image`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `car_id` bigint(0) NOT NULL,
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sort_order` int(0) NOT NULL DEFAULT 0,
  `source_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SERVER',
  `origin_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_car_image_car_id_sort`(`car_id`, `sort_order`, `id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of car_image
-- ----------------------------
INSERT INTO `car_image` VALUES (5, 24, '/static/car-images/CAR014/825b63c2-51ef-4851-9c86-9c51382f2ace.jpg', 0, 'SERVER', '/static/car-images/CAR014/825b63c2-51ef-4851-9c86-9c51382f2ace.jpg', '2026-05-14 20:31:19', '2026-05-15 17:36:49');
INSERT INTO `car_image` VALUES (7, 23, '/static/car-images/CAR013/df056810-fa4b-4dbf-bfc0-f61bc790fbb2.webp', 0, 'SERVER', '/static/car-images/CAR013/df056810-fa4b-4dbf-bfc0-f61bc790fbb2.webp', '2026-05-14 20:33:27', '2026-05-15 17:36:54');
INSERT INTO `car_image` VALUES (8, 22, '/static/car-images/CAR012/4a16d444-8995-4bdf-8b6a-8c244390c9b5.jpg', 0, 'SERVER', '/static/car-images/CAR012/4a16d444-8995-4bdf-8b6a-8c244390c9b5.jpg', '2026-05-14 20:34:16', '2026-05-15 17:36:58');
INSERT INTO `car_image` VALUES (9, 21, '/static/car-images/CAR011/86d08a51-20ba-4efd-a2e5-6806c21f8d42.jpg', 0, 'SERVER', '/static/car-images/CAR011/86d08a51-20ba-4efd-a2e5-6806c21f8d42.jpg', '2026-05-14 20:34:58', '2026-05-15 17:37:03');
INSERT INTO `car_image` VALUES (10, 20, '/static/car-images/CAR010/885f5e33-a57d-4b38-97e1-084bb46e5b97.jpg', 0, 'SERVER', '/static/car-images/CAR010/885f5e33-a57d-4b38-97e1-084bb46e5b97.jpg', '2026-05-14 20:35:41', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (11, 19, '/static/car-images/CAR009/482f0198-dc19-4ebc-84b9-51244faeffbc.jpg', 0, 'SERVER', '/static/car-images/CAR009/482f0198-dc19-4ebc-84b9-51244faeffbc.jpg', '2026-05-14 20:36:38', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (15, 16, '/static/car-images/CAR006/f971c963-82a9-4bbd-8627-118a620b8290.jpg', 0, 'SERVER', '/static/car-images/CAR006/f971c963-82a9-4bbd-8627-118a620b8290.jpg', '2026-05-14 20:38:30', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (16, 15, '/static/car-images/CAR005/49ef754d-546e-4425-bd98-7505af05d232.webp', 0, 'SERVER', '/static/car-images/CAR005/49ef754d-546e-4425-bd98-7505af05d232.webp', '2026-05-14 20:39:11', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (17, 17, '/static/car-images/CAR007/169f91bf-6c35-4951-8660-b16187cd1de2.jpg', 0, 'SERVER', '/static/car-images/CAR007/169f91bf-6c35-4951-8660-b16187cd1de2.jpg', '2026-05-14 20:40:18', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (18, 4, '/static/car-images/CAR004/09a75b20-23c9-4765-ad70-4185a9a1edc8.jpg', 0, 'SERVER', '/static/car-images/CAR004/09a75b20-23c9-4765-ad70-4185a9a1edc8.jpg', '2026-05-14 20:42:25', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (19, 3, '/static/car-images/CAR003/f23cdcd3-672d-458d-ba53-b3805da03021.jpg', 0, 'SERVER', '/static/car-images/CAR003/f23cdcd3-672d-458d-ba53-b3805da03021.jpg', '2026-05-14 20:43:09', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (20, 2, '/static/car-images/CAR002/fd0815ee-72ac-4681-a360-e5e4036d0b80.jpg', 0, 'SERVER', '/static/car-images/CAR002/fd0815ee-72ac-4681-a360-e5e4036d0b80.jpg', '2026-05-14 20:43:34', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (21, 1, '/static/car-images/CAR001/83bc03fc-c556-4113-a364-4217381344a1.jpg', 0, 'SERVER', '/static/car-images/CAR001/83bc03fc-c556-4113-a364-4217381344a1.jpg', '2026-05-14 20:43:53', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (24, 26, '/static/car-images/CAR000026/55c12f46-360b-4e6c-adf7-b47086da5885.webp', 0, 'SERVER', '/static/car-images/CAR000026/55c12f46-360b-4e6c-adf7-b47086da5885.webp', '2026-05-15 15:56:08', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (25, 18, '/static/car-images/CAR008/0bbd5116-d598-4ea4-bb5c-0b3358a2c56a.jpg', 0, 'SERVER', '/static/car-images/CAR008/0bbd5116-d598-4ea4-bb5c-0b3358a2c56a.jpg', '2026-05-15 16:45:06', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (26, 18, '/static/car-images/CAR008/cdecc6c7-0af0-4435-8b95-e3388d63ca31.jpg', 1, 'SERVER', '/static/car-images/CAR008/cdecc6c7-0af0-4435-8b95-e3388d63ca31.jpg', '2026-05-15 16:45:06', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (27, 27, '/static/car-images/CAR000027/155735b4-486a-47dd-9648-b4cafac31ec2.jpg', 0, 'SERVER', '/static/car-images/CAR000027/155735b4-486a-47dd-9648-b4cafac31ec2.jpg', '2026-05-15 16:48:14', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (28, 27, '/static/car-images/CAR000027/f3bac092-de9c-47cc-8393-8d4b3f749c6f.jpg', 1, 'SERVER', '/static/car-images/CAR000027/f3bac092-de9c-47cc-8393-8d4b3f749c6f.jpg', '2026-05-15 16:48:14', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (29, 27, '/static/car-images/CAR000027/03a6def8-49da-400f-be26-d563615cc442.jpg', 2, 'SERVER', '/static/car-images/CAR000027/03a6def8-49da-400f-be26-d563615cc442.jpg', '2026-05-15 16:48:14', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (30, 27, '/static/car-images/CAR000027/2fe60ac4-874f-4e00-868e-a827e4410ca4.jpg', 3, 'SERVER', '/static/car-images/CAR000027/2fe60ac4-874f-4e00-868e-a827e4410ca4.jpg', '2026-05-15 16:48:14', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (31, 27, '/static/car-images/CAR000027/1c75210b-44c4-404c-9856-5533772017ca.jpg', 4, 'SERVER', '/static/car-images/CAR000027/1c75210b-44c4-404c-9856-5533772017ca.jpg', '2026-05-15 16:48:14', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (32, 27, '/static/car-images/CAR000027/c1ea7d4e-ab78-47f8-a69c-e5d93ea30de1.jpg', 5, 'SERVER', '/static/car-images/CAR000027/c1ea7d4e-ab78-47f8-a69c-e5d93ea30de1.jpg', '2026-05-15 16:48:14', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (33, 25, '/static/car-images/CAR000025/0cfb411f-6de6-485f-8043-f8f9f096f0b8.webp', 0, 'SERVER', '/static/car-images/CAR000025/0cfb411f-6de6-485f-8043-f8f9f096f0b8.webp', '2026-05-15 16:48:28', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (34, 25, '/static/car-images/CAR000025/5281f265-91b5-41ac-bd36-a3ea458f578b.webp', 1, 'SERVER', '/static/car-images/CAR000025/5281f265-91b5-41ac-bd36-a3ea458f578b.webp', '2026-05-15 16:48:28', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (35, 25, '/static/car-images/CAR000025/f432b45f-2287-4b21-bdc7-da09ca8514a6.webp', 2, 'SERVER', '/static/car-images/CAR000025/f432b45f-2287-4b21-bdc7-da09ca8514a6.webp', '2026-05-15 16:48:28', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (36, 25, '/static/car-images/CAR000025/14cfc0de-cc85-4146-a7e1-b331ea7c22f2.webp', 3, 'SERVER', '/static/car-images/CAR000025/14cfc0de-cc85-4146-a7e1-b331ea7c22f2.webp', '2026-05-15 16:48:28', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (44, 29, '/static/car-images/CAR000029/ffccd422-3905-4a71-a008-fdd0fefa8100.jpg', 0, 'SERVER', '/static/car-images/CAR000029/ffccd422-3905-4a71-a008-fdd0fefa8100.jpg', '2026-05-15 17:00:57', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (45, 29, '/static/car-images/CAR000029/fe28f8a1-e565-4187-ae8f-f0b3fed6434c.jpg', 1, 'SERVER', '/static/car-images/CAR000029/fe28f8a1-e565-4187-ae8f-f0b3fed6434c.jpg', '2026-05-15 17:00:57', '2026-05-15 17:37:34');
INSERT INTO `car_image` VALUES (47, 30, '/static/car-images/CAR000030/1216b25b-7d84-4bd2-bafa-f7816dc3acb0.jpg', 0, 'SERVER', '/static/car-images/CAR000030/1216b25b-7d84-4bd2-bafa-f7816dc3acb0.jpg', '2026-05-15 17:20:47', '2026-05-15 17:20:47');
INSERT INTO `car_image` VALUES (48, 30, '/static/car-images/CAR000030/183fe35b-118b-4172-9b4c-d9f47a5538d4.jpg', 1, 'SERVER', '/static/car-images/CAR000030/183fe35b-118b-4172-9b4c-d9f47a5538d4.jpg', '2026-05-15 17:20:47', '2026-05-15 17:20:47');
INSERT INTO `car_image` VALUES (49, 30, '/static/car-images/CAR000030/b862cd62-e96c-4943-8750-af51b0abd09c.jpg', 2, 'SERVER', '/static/car-images/CAR000030/b862cd62-e96c-4943-8750-af51b0abd09c.jpg', '2026-05-15 17:20:47', '2026-05-15 17:20:47');
INSERT INTO `car_image` VALUES (50, 30, '/static/car-images/CAR000030/492bef1b-8727-421e-bfcf-bf041e330b7e.jpg', 3, 'SERVER', '/static/car-images/CAR000030/492bef1b-8727-421e-bfcf-bf041e330b7e.jpg', '2026-05-15 17:20:47', '2026-05-15 17:20:47');
INSERT INTO `car_image` VALUES (51, 30, '/static/car-images/CAR000030/6bd2027d-973c-45ee-b815-e0d21838f4bc.jpg', 4, 'SERVER', '/static/car-images/CAR000030/6bd2027d-973c-45ee-b815-e0d21838f4bc.jpg', '2026-05-15 17:20:47', '2026-05-15 17:20:47');
INSERT INTO `car_image` VALUES (54, 32, '/static/car-images/CAR000032/5f46de27-ee2c-4a86-b057-39cb25a04109.jpg', 0, 'SERVER', '/static/car-images/_temp/5f46de27-ee2c-4a86-b057-39cb25a04109.jpg', '2026-05-15 17:50:11', '2026-05-15 17:50:11');
INSERT INTO `car_image` VALUES (55, 32, '/static/car-images/CAR000032/2139a748-627c-47e1-b01d-b59cd263845c.jpg', 1, 'SERVER', '/static/car-images/_temp/2139a748-627c-47e1-b01d-b59cd263845c.jpg', '2026-05-15 17:50:11', '2026-05-15 17:50:11');
INSERT INTO `car_image` VALUES (56, 32, '/static/car-images/CAR000032/3c04d87c-d123-4b2d-8d60-b2927361d22f.jpg', 2, 'SERVER', '/static/car-images/_temp/3c04d87c-d123-4b2d-8d60-b2927361d22f.jpg', '2026-05-15 17:50:11', '2026-05-15 17:50:11');
INSERT INTO `car_image` VALUES (57, 32, '/static/car-images/CAR000032/c48d34f2-a581-4919-a001-a2a63fca8e4c.jpg', 3, 'SERVER', '/static/car-images/_temp/c48d34f2-a581-4919-a001-a2a63fca8e4c.jpg', '2026-05-15 17:50:11', '2026-05-15 17:50:11');
INSERT INTO `car_image` VALUES (58, 32, '/static/car-images/CAR000032/9816908e-f7bd-45ce-90fe-c89efcfd5fe7.jpg', 4, 'SERVER', '/static/car-images/_temp/9816908e-f7bd-45ce-90fe-c89efcfd5fe7.jpg', '2026-05-15 17:50:11', '2026-05-15 17:50:11');
INSERT INTO `car_image` VALUES (59, 33, '/static/car-images/CAR000033/7925c24c-94f6-4c30-b95d-09016fb16b74.jpg', 0, 'SERVER', '/static/car-images/_temp/7925c24c-94f6-4c30-b95d-09016fb16b74.jpg', '2026-05-15 17:57:16', '2026-05-15 17:57:16');
INSERT INTO `car_image` VALUES (60, 33, '/static/car-images/CAR000033/bf0774ee-beb0-4ae1-9fdb-eb1cb4cdc249.jpg', 1, 'SERVER', '/static/car-images/_temp/bf0774ee-beb0-4ae1-9fdb-eb1cb4cdc249.jpg', '2026-05-15 17:57:16', '2026-05-15 17:57:16');
INSERT INTO `car_image` VALUES (61, 33, '/static/car-images/CAR000033/364f6bac-9952-4d46-ae43-390656ea4f8b.jpg', 2, 'SERVER', '/static/car-images/_temp/364f6bac-9952-4d46-ae43-390656ea4f8b.jpg', '2026-05-15 17:57:16', '2026-05-15 17:57:16');
INSERT INTO `car_image` VALUES (62, 33, '/static/car-images/CAR000033/6699deab-9def-4085-beea-5ec5e141d075.jpg', 3, 'SERVER', '/static/car-images/_temp/6699deab-9def-4085-beea-5ec5e141d075.jpg', '2026-05-15 17:57:16', '2026-05-15 17:57:16');
INSERT INTO `car_image` VALUES (63, 33, '/static/car-images/CAR000033/d1a011cf-89cc-4569-8180-079386211ca0.jpg', 4, 'SERVER', '/static/car-images/_temp/d1a011cf-89cc-4569-8180-079386211ca0.jpg', '2026-05-15 17:57:16', '2026-05-15 17:57:16');
INSERT INTO `car_image` VALUES (64, 33, '/static/car-images/CAR000033/c4128392-8d6a-4f54-819c-b2839e969018.jpg', 5, 'SERVER', '/static/car-images/_temp/c4128392-8d6a-4f54-819c-b2839e969018.jpg', '2026-05-15 17:57:16', '2026-05-15 17:57:16');
INSERT INTO `car_image` VALUES (65, 33, '/static/car-images/CAR000033/3ded8e39-b4c7-4325-8cb4-bb8eb3ee6aee.jpg', 6, 'SERVER', '/static/car-images/_temp/3ded8e39-b4c7-4325-8cb4-bb8eb3ee6aee.jpg', '2026-05-15 17:57:16', '2026-05-15 17:57:16');
INSERT INTO `car_image` VALUES (66, 33, '/static/car-images/CAR000033/dc674303-0464-40c7-ac27-cedf6cfd1cc0.jpg', 7, 'SERVER', '/static/car-images/_temp/dc674303-0464-40c7-ac27-cedf6cfd1cc0.jpg', '2026-05-15 17:57:16', '2026-05-15 17:57:16');

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
  `province` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `detail_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `pickup_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `car_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'AVAILABLE',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `car_no`(`car_no`) USING BTREE,
  UNIQUE INDEX `plate_number`(`plate_number`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of car_info
-- ----------------------------
INSERT INTO `car_info` VALUES (1, 'CAR001', 1, '大众', '朗逸 2024', '鲁A12345', 180.00, 12100, '山东省', '济南市', '山东建筑大学新校区东门', '山东省济南市山东建筑大学新校区东门', '/static/car-images/202605/83bc03fc-c556-4113-a364-4217381344a1.jpg', 'AVAILABLE', '2026-03-08 23:12:23', '2026-05-14 20:43:53');
INSERT INTO `car_info` VALUES (2, 'CAR002', 2, '哈弗', 'H6 2023', '鲁A23456', 260.00, 20, '山东省', '济南市', '济南西站停车场', '山东省济南市济南西站停车场', '/static/car-images/202605/fd0815ee-72ac-4681-a360-e5e4036d0b80.jpg', 'RESERVED', '2026-03-08 23:12:23', '2026-05-14 20:43:34');
INSERT INTO `car_info` VALUES (3, 'CAR003', 3, '比亚迪', '秦PLUS EV', '鲁A34567', 220.00, 18000, '山东省', '济南市', '泉城广场地下一层', '山东省济南市泉城广场地下一层', '/static/car-images/202605/f23cdcd3-672d-458d-ba53-b3805da03021.jpg', 'AVAILABLE', '2026-03-08 23:12:23', '2026-05-14 20:43:09');
INSERT INTO `car_info` VALUES (4, 'CAR004', 3, '小米', 'SU7', '鲁G88888', 300.00, 0, '山东省', '潍坊市', '潍城区', '山东省潍坊市潍城区', '/static/car-images/202605/09a75b20-23c9-4765-ad70-4185a9a1edc8.jpg', 'AVAILABLE', '2026-05-02 20:38:50', '2026-05-14 20:42:25');
INSERT INTO `car_info` VALUES (15, 'CAR005', 1, '丰田', '卡罗拉 2024', '鲁A56789', 160.00, 8500, '山东省', '济南市', '济南遥墙机场停车楼', '山东省济南市济南遥墙机场停车楼', '/static/car-images/202605/49ef754d-546e-4425-bd98-7505af05d232.webp', 'AVAILABLE', '2026-05-03 14:31:24', '2026-05-14 20:39:11');
INSERT INTO `car_info` VALUES (16, 'CAR006', 2, '本田', 'CR-V 2024', '鲁A67890', 280.00, 15200, '山东省', '济南市', '山东建筑大学新校区东门', '山东省济南市山东建筑大学新校区东门', '/static/car-images/202605/f971c963-82a9-4bbd-8627-118a620b8290.jpg', 'AVAILABLE', '2026-05-03 14:31:24', '2026-05-14 20:38:30');
INSERT INTO `car_info` VALUES (17, 'CAR007', 3, '特斯拉', 'Model 3 2024', '鲁A78901', 350.00, 6241, '山东省', '济南市', '济南万象城地下停车场', '山东省济南市济南万象城地下停车场', '/static/car-images/202605/169f91bf-6c35-4951-8660-b16187cd1de2.jpg', 'AVAILABLE', '2026-05-03 14:31:24', '2026-05-14 20:40:18');
INSERT INTO `car_info` VALUES (18, 'CAR008', 4, '宝马', '3系 2023', '鲁A89012', 420.00, 22000, '山东省', '济南市', '济南西站停车场', '山东省济南市济南西站停车场', '/static/car-images/202605/0bbd5116-d598-4ea4-bb5c-0b3358a2c56a.jpg', 'AVAILABLE', '2026-05-03 14:31:24', '2026-05-15 16:45:06');
INSERT INTO `car_info` VALUES (19, 'CAR009', 2, '奥迪', 'Q5L 2024', '鲁A90123', 480.00, 9800, '山东省', '济南市', '泉城广场地下一层', '山东省济南市泉城广场地下一层', '/static/car-images/202605/482f0198-dc19-4ebc-84b9-51244faeffbc.jpg', 'REPAIRING', '2026-05-03 14:31:24', '2026-05-14 20:36:38');
INSERT INTO `car_info` VALUES (20, 'CAR010', 3, '蔚来', 'ET5 2024', '鲁A01234', 320.00, 4678, '山东省', '济南市', '济南奥体中心停车场', '山东省济南市济南奥体中心停车场', '/static/car-images/202605/885f5e33-a57d-4b38-97e1-084bb46e5b97.jpg', 'AVAILABLE', '2026-05-03 14:31:24', '2026-05-14 20:35:41');
INSERT INTO `car_info` VALUES (21, 'CAR011', 1, '奔驰', 'C级 2024', '鲁A11234', 450.00, 11234, '山东省', '济南市', '山东建筑大学新校区东门', '山东省济南市山东建筑大学新校区东门', '/static/car-images/202605/86d08a51-20ba-4efd-a2e5-6806c21f8d42.jpg', 'AVAILABLE', '2026-05-03 14:31:24', '2026-05-14 20:34:58');
INSERT INTO `car_info` VALUES (22, 'CAR012', 2, '长安', 'CS75 PLUS 2024', '鲁A22345', 200.00, 18500, '山东省', '济南市', '济南长途汽车总站', '山东省济南市济南长途汽车总站', '/static/car-images/202605/4a16d444-8995-4bdf-8b6a-8c244390c9b5.jpg', 'DISABLED', '2026-05-03 14:31:24', '2026-05-14 20:34:16');
INSERT INTO `car_info` VALUES (23, 'CAR013', 3, '小鹏', 'P7 2024', '鲁A33456', 300.00, 7809, '山东省', '济南市', '济南万象城地下停车场', '山东省济南市济南万象城地下停车场', '/static/car-images/202605/df056810-fa4b-4dbf-bfc0-f61bc790fbb2.webp', 'AVAILABLE', '2026-05-03 14:31:24', '2026-05-14 20:33:27');
INSERT INTO `car_info` VALUES (24, 'CAR014', 1, '大众', '帕萨特 2024', '鲁A44567', 220.00, 14000, '山东省', '济南市', '济南遥墙机场停车楼', '山东省济南市济南遥墙机场停车楼', '/static/car-images/202605/825b63c2-51ef-4851-9c86-9c51382f2ace.jpg', 'AVAILABLE', '2026-05-03 14:31:24', '2026-05-14 20:31:19');
INSERT INTO `car_info` VALUES (25, 'CAR000025', 3, '小米', 'SU7', '鲁A888888', 288.00, 1000, '山东省', '济宁市', '港沟街道lzhgod办事处', '山东省济宁市港沟街道lzhgod办事处', '/static/car-images/202605/0cfb411f-6de6-485f-8043-f8f9f096f0b8.webp', 'RESERVED', '2026-05-14 20:15:35', '2026-05-15 16:48:28');
INSERT INTO `car_info` VALUES (26, 'CAR000026', 4, '凯迪拉克', 'CT5', '新A88888', 300.00, 23575, '新疆维吾尔自治区', '乌鲁木齐市', '奥体中心停车场', '新疆维吾尔自治区乌鲁木齐市奥体中心停车场', '/static/car-images/202605/55c12f46-360b-4e6c-adf7-b47086da5885.webp', 'RESERVED', '2026-05-15 15:56:08', '2026-05-15 15:56:08');
INSERT INTO `car_info` VALUES (27, 'CAR000027', 2, '问界', 'M9', '鲁B999999', 220.00, 10, '山东省', '青岛市', '泉城广场', '山东省青岛市泉城广场', '/static/car-images/202605/155735b4-486a-47dd-9648-b4cafac31ec2.jpg', 'RESERVED', '2026-05-15 16:48:14', '2026-05-15 16:48:14');
INSERT INTO `car_info` VALUES (29, 'CAR000029', 2, '问界', 'M7', '鲁C88888', 270.00, 123123120, '山东省', '淄博市', '万达广场地下停车场', '山东省淄博市万达广场地下停车场', '/static/car-images/202605/ffccd422-3905-4a71-a008-fdd0fefa8100.jpg', 'RESERVED', '2026-05-15 17:00:48', '2026-05-15 17:00:57');
INSERT INTO `car_info` VALUES (30, 'CAR000030', 3, '享界', 'S9T', '鲁A43563', 453.00, 2131243124, '山东省', '济南市', '融创茂停车场', '山东省济南市融创茂停车场', '/static/car-images/CAR000030/1216b25b-7d84-4bd2-bafa-f7816dc3acb0.jpg', 'AVAILABLE', '2026-05-15 17:20:08', '2026-05-15 17:20:08');
INSERT INTO `car_info` VALUES (32, 'CAR000032', 1, '享界', 'Z7', '晋A555666', 343.00, 24553, '山西省', '太原市', '万达广场停车场C区', '山西省太原市万达广场停车场C区', '/static/car-images/CAR000032/5f46de27-ee2c-4a86-b057-39cb25a04109.jpg', 'RESERVED', '2026-05-15 17:50:11', '2026-05-15 17:50:11');
INSERT INTO `car_info` VALUES (33, 'CAR000033', 2, '小米', 'YU7', '鲁A666666', 388.00, 2342, '山东省', '济南市', '山东建筑大学小西门', '山东省济南市山东建筑大学小西门', '/static/car-images/CAR000033/7925c24c-94f6-4c30-b95d-09016fb16b74.jpg', 'RENTED', '2026-05-15 17:57:16', '2026-05-15 17:57:16');

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
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of car_type
-- ----------------------------
INSERT INTO `car_type` VALUES (1, '轿车', '适合城市通勤与家庭出行', '2026-03-08 23:12:23', '2026-03-08 23:12:23');
INSERT INTO `car_type` VALUES (2, 'SUV', '适合长途出行与复杂路况', '2026-03-08 23:12:23', '2026-03-08 23:12:23');
INSERT INTO `car_type` VALUES (3, '新能源', '节能环保，适合短中途使用', '2026-03-08 23:12:23', '2026-03-08 23:12:23');
INSERT INTO `car_type` VALUES (4, '燃油车', '', '2026-05-14 20:13:02', '2026-05-14 20:13:02');

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
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

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
INSERT INTO `fault_report` VALUES (8, 2, 3, 'Battery warning', 'RESOLVED', '已安排维修', '2026-03-08 09:30:00', '2026-03-23 15:51:16', '2026-03-08 09:30:00', '2026-03-08 09:30:00');
INSERT INTO `fault_report` VALUES (9, 3, 1, 'Brake noise', 'RESOLVED', 'Scheduled maintenance', '2026-03-10 13:20:00', '2026-03-23 15:51:15', '2026-03-10 13:20:00', '2026-03-10 15:00:00');
INSERT INTO `fault_report` VALUES (10, 1, 4, '电池爆炸了', 'RESOLVED', '已安排维修', '2026-05-03 18:10:23', '2026-05-03 18:10:47', '2026-05-03 18:10:23', '2026-05-03 18:10:23');
INSERT INTO `fault_report` VALUES (11, 1, 4, '崴脚了', 'RESOLVED', '已安排维修', '2026-05-03 18:11:16', '2026-05-03 18:11:21', '2026-05-03 18:11:16', '2026-05-03 18:11:16');
INSERT INTO `fault_report` VALUES (12, 3, 17, '空调坏了', 'RESOLVED', '已安排维修', '2026-05-12 15:42:54', '2026-05-12 15:43:18', '2026-05-12 15:42:53', '2026-05-12 15:42:53');
INSERT INTO `fault_report` VALUES (13, 3, 17, '空调坏了', 'RESOLVED', '已完成', '2026-05-12 15:53:19', '2026-05-12 15:54:24', '2026-05-12 15:53:18', '2026-05-12 15:53:18');
INSERT INTO `fault_report` VALUES (14, 3, 24, '车坏了', 'REJECTED', '故障报告不成立', '2026-05-14 15:09:57', '2026-05-14 21:57:33', '2026-05-14 15:09:57', '2026-05-14 15:09:57');
INSERT INTO `fault_report` VALUES (15, 3, 19, '车坏了', 'REPAIRING', '已安排维修', '2026-05-14 15:10:03', '2026-05-14 15:10:15', '2026-05-14 15:10:02', '2026-05-14 15:10:02');
INSERT INTO `fault_report` VALUES (16, 3, 25, '后视镜掉了', 'RESOLVED', '完成维修', '2026-05-15 17:43:23', '2026-05-15 17:58:27', '2026-05-15 17:43:23', '2026-05-15 17:43:23');
INSERT INTO `fault_report` VALUES (17, 3, 17, '方向盘断了', 'RESOLVED', '已修复', '2026-05-16 14:39:01', '2026-05-16 14:39:19', '2026-05-16 14:39:01', '2026-05-16 14:39:01');

-- ----------------------------
-- Table structure for message_notice
-- ----------------------------
DROP TABLE IF EXISTS `message_notice`;
CREATE TABLE `message_notice`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `receiver_id` bigint(0) NOT NULL,
  `sender_id` bigint(0) NULL DEFAULT NULL,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `message_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `biz_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `biz_id` bigint(0) NULL DEFAULT NULL,
  `read_status` tinyint(0) NOT NULL DEFAULT 0,
  `read_time` datetime(0) NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_message_notice_receiver`(`receiver_id`, `id`) USING BTREE,
  INDEX `idx_message_notice_unread`(`receiver_id`, `read_status`, `id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 99 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of message_notice
-- ----------------------------
INSERT INTO `message_notice` VALUES (1, 1, 3, '新租车订单', '李四 提交了租车订单 RENT1778912270846，车辆：小米 YU7', 'RENT_ORDER_CREATED', 'RENT_ORDER', 40, 1, '2026-05-16 14:18:15', '2026-05-16 14:17:50', '2026-05-16 14:17:50');
INSERT INTO `message_notice` VALUES (3, 1, 3, '新租车订单', '李四 提交了租车订单 RENT1778913090841，车辆：小米 YU7', 'RENT_ORDER_CREATED', 'RENT_ORDER', 41, 1, '2026-05-16 17:17:26', '2026-05-16 14:31:30', '2026-05-16 17:17:26');
INSERT INTO `message_notice` VALUES (4, 1, 3, '新租车订单', '李四 提交了租车订单 RENT1778913106466，车辆：享界 S9T', 'RENT_ORDER_CREATED', 'RENT_ORDER', 42, 1, '2026-05-16 14:32:30', '2026-05-16 14:31:46', '2026-05-16 14:31:46');
INSERT INTO `message_notice` VALUES (5, 1, 3, '还车申请提醒', '李四 提交了还车申请，订单号：RENT1778743706570，车辆：特斯拉 Model 3 2024', 'RETURN_ORDER_CREATED', 'RETURN_ORDER', 23, 1, '2026-05-16 14:37:56', '2026-05-16 14:37:46', '2026-05-16 14:37:46');
INSERT INTO `message_notice` VALUES (6, 1, 3, '故障报修提醒', '李四 提交了故障报修，车辆：特斯拉 Model 3 2024', 'FAULT_REPORT_CREATED', 'FAULT_REPORT', 17, 1, '2026-05-16 14:39:07', '2026-05-16 14:39:01', '2026-05-16 14:39:01');
INSERT INTO `message_notice` VALUES (18, 1, 3, '新租车订单', '李四 提交了租车订单 RENT1778920216498，车辆：小米 SU7', 'RENT_ORDER_CREATED', 'RENT_ORDER', 43, 1, '2026-05-16 17:17:26', '2026-05-16 16:30:16', '2026-05-16 17:17:26');
INSERT INTO `message_notice` VALUES (58, 1, 3, '新租车订单', '李四 提交了租车订单 RENT1778929768421，车辆：小米 YU7', 'RENT_ORDER_CREATED', 'RENT_ORDER', 44, 1, '2026-05-16 19:09:30', '2026-05-16 19:09:28', '2026-05-16 19:09:28');
INSERT INTO `message_notice` VALUES (60, 1, 3, '还车申请提醒', '李四 提交了还车申请，订单号：RENT1778381623424，车辆：蔚来 ET5 2024', 'RETURN_ORDER_CREATED', 'RETURN_ORDER', 24, 1, '2026-05-16 19:18:26', '2026-05-16 19:17:50', '2026-05-16 19:17:50');
INSERT INTO `message_notice` VALUES (78, 1, 3, '新租车订单', '李四 提交了租车订单 RENT1779289517825，车辆：小米 YU7', 'RENT_ORDER_CREATED', 'RENT_ORDER', 45, 1, '2026-05-20 23:06:09', '2026-05-20 23:05:17', '2026-05-20 23:05:17');
INSERT INTO `message_notice` VALUES (79, 1, 3, '新租车订单', '李四 提交了租车订单 RENT1779289521340，车辆：享界 Z7', 'RENT_ORDER_CREATED', 'RENT_ORDER', 46, 1, '2026-05-20 23:06:08', '2026-05-20 23:05:21', '2026-05-20 23:05:21');
INSERT INTO `message_notice` VALUES (80, 3, 1, '还车提醒', '管理员提醒您尽快归还小米 YU7，车牌号为鲁A666666', 'RETURN_REMINDER', 'RENT_ORDER', 45, 1, '2026-05-20 23:05:51', '2026-05-20 23:05:39', '2026-05-20 23:05:39');
INSERT INTO `message_notice` VALUES (82, 1, 3, '还车申请提醒', '李四 提交了还车申请，订单号：RENT1778381212175，车辆：奔驰 C级 2024', 'RETURN_ORDER_CREATED', 'RETURN_ORDER', 25, 1, '2026-05-20 23:27:44', '2026-05-20 23:06:28', '2026-05-20 23:06:28');
INSERT INTO `message_notice` VALUES (90, 1, 3, '新租车订单', '李四 提交了租车订单 RENT1779326473392，车辆：问界 M7', 'RENT_ORDER_CREATED', 'RENT_ORDER', 47, 1, '2026-06-15 22:18:02', '2026-05-21 09:21:13', '2026-06-15 22:18:02');
INSERT INTO `message_notice` VALUES (91, 1, 3, '新租车订单', '李四 提交了租车订单 RENT1779798216244，车辆：享界 S9T', 'RENT_ORDER_CREATED', 'RENT_ORDER', 48, 1, '2026-06-15 22:18:02', '2026-05-26 20:23:36', '2026-06-15 22:18:02');
INSERT INTO `message_notice` VALUES (92, 1, 3, '新租车订单', '李四 提交了租车订单 RENT1779798518374，车辆：享界 S9T', 'RENT_ORDER_CREATED', 'RENT_ORDER', 49, 1, '2026-06-15 22:18:02', '2026-05-26 20:28:38', '2026-06-15 22:18:02');
INSERT INTO `message_notice` VALUES (93, 1, 3, '新租车订单', '李四 提交了租车订单 RENT1779798851121，车辆：凯迪拉克 CT5', 'RENT_ORDER_CREATED', 'RENT_ORDER', 50, 1, '2026-06-15 22:18:02', '2026-05-26 20:34:11', '2026-06-15 22:18:02');
INSERT INTO `message_notice` VALUES (94, 1, 3, '新租车订单', '李四 提交了租车订单 RENT1779799251761，车辆：享界 S9T', 'RENT_ORDER_CREATED', 'RENT_ORDER', 51, 1, '2026-06-15 22:18:02', '2026-05-26 20:40:51', '2026-06-15 22:18:02');
INSERT INTO `message_notice` VALUES (95, 1, 3, '还车申请提醒', '李四 提交了还车申请，订单号：RENT1779289517825，车辆：小米 YU7', 'RETURN_ORDER_CREATED', 'RETURN_ORDER', 26, 1, '2026-06-15 22:18:02', '2026-05-26 20:49:57', '2026-06-15 22:18:02');
INSERT INTO `message_notice` VALUES (98, 1, 3, '新租车订单', '李四 提交了租车订单 RENT1781532868700，车辆：问界 M9', 'RENT_ORDER_CREATED', 'RENT_ORDER', 52, 1, '2026-06-15 22:18:02', '2026-06-15 22:14:28', '2026-06-15 22:18:02');
INSERT INTO `message_notice` VALUES (99, 1, 3, '新租车订单', '李四 提交了租车订单 RENT1781533125846，车辆：小米 SU7', 'RENT_ORDER_CREATED', 'RENT_ORDER', 53, 0, NULL, '2026-06-15 22:18:45', '2026-06-15 22:18:45');

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
  `payment_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'UNPAID',
  `payment_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `payment_time` datetime(0) NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_no`(`order_no`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 53 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of rent_order
-- ----------------------------
INSERT INTO `rent_order` VALUES (2, 'RENT1773026775825', 3, 2, '2026-03-09', '2026-03-11', '2026-03-09', 2, 260.00, 520.00, 'COMPLETED', 'PAID', NULL, NULL, '', '2026-03-09 11:26:15', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (3, 'RENT1773026865687', 3, 2, '2026-03-09', '2026-03-11', '2026-03-09', 2, 260.00, 520.00, 'COMPLETED', 'PAID', NULL, NULL, '', '2026-03-09 11:27:45', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (4, 'RENT1773028182194', 3, 2, '2026-03-09', '2026-03-12', '2026-03-12', 3, 260.00, 780.00, 'COMPLETED', 'PAID', NULL, NULL, '', '2026-03-09 11:49:42', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (5, 'RENT1773286879685', 3, 2, '2026-03-13', '2026-03-15', '2026-03-12', 2, 260.00, 520.00, 'COMPLETED', 'PAID', NULL, NULL, '', '2026-03-12 11:41:19', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (6, 'RENT1774233747990', 3, 2, '2026-03-23', '2026-03-25', '2026-03-23', 2, 260.00, 520.00, 'COMPLETED', 'PAID', NULL, NULL, '', '2026-03-21 10:42:27', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (7, 'RENT1774233803444', 3, 2, '2026-03-23', '2026-03-25', '2026-03-23', 2, 260.00, 520.00, 'COMPLETED', 'PAID', NULL, NULL, '', '2026-03-22 10:43:23', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (8, 'RENT1774247034828', 3, 1, '2026-03-24', '2026-03-26', '2026-03-23', 2, 180.00, 360.00, 'COMPLETED', 'PAID', NULL, NULL, '', '2026-03-23 14:23:54', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (9, 'RENT202603080001', 2, 2, '2026-03-06', '2026-03-10', NULL, 4, 260.00, 1040.00, 'RENTED', 'PAID', NULL, NULL, 'Weekend drive', '2026-03-08 10:10:00', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (10, 'RENT202603170001', 2, 1, '2026-03-17', '2026-03-20', NULL, 3, 180.00, 540.00, 'RENTED', 'PAID', NULL, NULL, 'Spring trip', '2026-03-17 09:12:00', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (11, 'RENT202603160001', 3, 2, '2026-03-16', '2026-03-18', '2026-03-18', 2, 260.00, 520.00, 'COMPLETED', 'PAID', NULL, NULL, 'Business', '2026-03-16 08:20:00', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (12, 'RENT202603150001', 2, 3, '2026-03-15', '2026-03-19', '2026-03-19', 4, 220.00, 880.00, 'COMPLETED', 'PAID', NULL, NULL, 'Family visit', '2026-03-15 10:05:00', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (13, 'RENT202603090001', 3, 1, '2026-03-09', '2026-03-11', '2026-03-11', 2, 180.00, 360.00, 'COMPLETED', 'PAID', NULL, NULL, 'Short trip', '2026-03-09 11:00:00', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (14, 'RENT202602120001', 2, 2, '2026-02-12', '2026-02-15', '2026-02-15', 3, 260.00, 780.00, 'COMPLETED', 'PAID', NULL, NULL, 'Weekend', '2026-02-12 09:00:00', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (15, 'RENT202601050001', 3, 3, '2026-01-05', '2026-01-07', '2026-01-07', 2, 220.00, 440.00, 'COMPLETED', 'PAID', NULL, NULL, 'City run', '2026-01-05 08:10:00', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (16, 'RENT202512200001', 2, 1, '2025-12-20', '2025-12-22', '2025-12-22', 2, 180.00, 360.00, 'COMPLETED', 'PAID', NULL, NULL, 'Holiday', '2025-12-20 10:30:00', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (17, 'RENT202407080001', 3, 2, '2024-07-08', '2024-07-10', '2024-07-10', 2, 260.00, 520.00, 'COMPLETED', 'PAID', NULL, NULL, 'Summer', '2024-07-08 09:40:00', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (18, 'RENT202303150001', 2, 3, '2023-03-15', '2023-03-18', '2023-03-18', 3, 220.00, 660.00, 'COMPLETED', 'PAID', NULL, NULL, 'Old data', '2023-03-15 13:20:00', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (19, 'RENT202201120001', 3, 1, '2022-01-12', '2022-01-14', '2022-01-14', 2, 180.00, 360.00, 'COMPLETED', 'PAID', NULL, NULL, 'Legacy', '2022-01-12 08:00:00', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (20, 'RENT1776751853353', 3, 2, '2026-04-22', '2026-04-23', '2026-04-21', 1, 260.00, 260.00, 'COMPLETED', 'PAID', NULL, NULL, '租车', '2026-04-21 14:10:53', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (21, 'RENT1777357446534', 3, 1, '2026-04-28', '2026-04-30', '2026-05-02', 2, 180.00, 360.00, 'COMPLETED', 'PAID', NULL, NULL, '', '2026-04-28 14:24:06', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (22, 'RENT1777807901536', 3, 23, '2026-05-04', '2026-05-07', '2026-05-03', 3, 300.00, 900.00, 'COMPLETED', 'PAID', NULL, NULL, '', '2026-05-03 19:31:41', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (23, 'RENT1777811477280', 3, 17, '2026-05-03', '2026-05-05', '2026-05-09', 2, 350.00, 700.00, 'COMPLETED', 'PAID', NULL, NULL, '', '2026-05-03 20:31:17', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (24, 'RENT1778310397222', 3, 17, '2026-05-10', '2026-05-12', NULL, 2, 350.00, 700.00, 'CANCELLED', 'PAID', NULL, NULL, '租车', '2026-05-09 15:06:37', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (25, 'RENT1778310418211', 3, 17, '2026-05-09', '2026-05-14', '2026-05-09', 5, 350.00, 1750.00, 'COMPLETED', 'PAID', NULL, NULL, '111', '2026-05-09 15:06:58', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (26, 'RENT1778332500359', 3, 23, '2026-05-10', '2026-05-13', '2026-05-10', 3, 300.00, 900.00, 'COMPLETED', 'PAID', NULL, NULL, '', '2026-05-09 21:15:00', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (27, 'RENT1778381212175', 3, 21, '2026-05-11', '2026-05-14', '2026-05-26', 3, 450.00, 1350.00, 'COMPLETED', 'PAID', NULL, NULL, '', '2026-05-10 10:46:52', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (28, 'RENT1778381623424', 3, 20, '2026-05-11', '2026-05-13', '2026-05-16', 2, 320.00, 640.00, 'COMPLETED', 'PAID', NULL, NULL, '', '2026-05-10 10:53:43', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (29, 'RENT1778382393497', 3, 19, '2026-05-11', '2026-05-13', NULL, 2, 480.00, 960.00, 'CANCELLED', 'PAID', NULL, NULL, '', '2026-05-10 11:06:33', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (30, 'RENT1778397107602', 3, 19, '2026-05-11', '2026-05-13', NULL, 2, 480.00, 960.00, 'CANCELLED', 'PAID', NULL, NULL, '', '2026-05-10 15:11:47', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (31, 'RENT1778412177235', 7, 2, '2026-05-11', '2026-05-30', NULL, 19, 260.00, 4940.00, 'PENDING_PICKUP', 'PAID', NULL, NULL, '', '2026-05-10 19:22:57', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (32, 'RENT1778499404299', 4, 3, '2026-05-12', '2026-05-14', NULL, 2, 220.00, 440.00, 'CANCELLED', 'PAID', NULL, NULL, '', '2026-05-11 19:36:44', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (33, 'RENT1778597691292', 8, 23, '2026-05-13', '2026-05-15', NULL, 2, 300.00, 600.00, 'CANCELLED', 'PAID', NULL, NULL, '', '2026-05-12 22:54:51', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (34, 'RENT1778677746537', 3, 24, '2026-05-14', '2026-05-17', NULL, 3, 220.00, 660.00, 'CANCELLED', 'PAID', NULL, NULL, '', '2026-05-13 21:09:06', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (35, 'RENT1778678455076', 3, 24, '2026-05-14', '2026-05-17', NULL, 3, 220.00, 660.00, 'CANCELLED', 'PAID', NULL, NULL, '', '2026-05-13 21:20:55', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (36, 'RENT1778742756968', 3, 23, '2026-05-15', '2026-05-17', NULL, 2, 300.00, 600.00, 'CANCELLED', 'PAID', NULL, NULL, '', '2026-05-14 15:12:36', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (37, 'RENT1778743706570', 3, 17, '2026-05-15', '2026-05-18', '2026-05-16', 3, 350.00, 1050.00, 'COMPLETED', 'PAID', NULL, NULL, '', '2026-05-14 15:28:26', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (38, 'RENT1778757519041', 3, 4, '2026-05-15', '2026-05-18', NULL, 3, 300.00, 900.00, 'CANCELLED', 'PAID', NULL, NULL, '哈哈哈这是备注测试', '2026-05-14 19:18:39', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (39, 'RENT1778819788890', 3, 25, '2026-05-16', '2026-05-22', NULL, 6, 288.00, 1728.00, 'CANCELLED', 'PAID', NULL, NULL, '', '2026-05-15 12:36:28', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (40, 'RENT1778912270846', 3, 33, '2026-05-16', '2026-05-17', NULL, 1, 388.00, 388.00, 'CANCELLED', 'PAID', NULL, NULL, '', '2026-05-16 14:17:50', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (41, 'RENT1778913090841', 3, 33, '2026-05-16', '2026-05-17', NULL, 1, 388.00, 388.00, 'CANCELLED', 'PAID', NULL, NULL, '', '2026-05-16 14:31:30', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (42, 'RENT1778913106466', 3, 30, '2026-05-16', '2026-05-17', NULL, 1, 453.00, 453.00, 'CANCELLED', 'PAID', NULL, NULL, '', '2026-05-16 14:31:46', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (43, 'RENT1778920216498', 3, 4, '2026-05-16', '2026-05-17', NULL, 1, 300.00, 300.00, 'CANCELLED', 'PAID', NULL, NULL, '', '2026-05-16 16:30:16', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (44, 'RENT1778929768421', 3, 33, '2026-05-16', '2026-05-17', NULL, 1, 388.00, 388.00, 'CANCELLED', 'PAID', NULL, NULL, '', '2026-05-16 19:09:28', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (45, 'RENT1779289517825', 3, 33, '2026-05-20', '2026-05-21', NULL, 1, 388.00, 388.00, 'RETURN_PENDING', 'PAID', NULL, NULL, '', '2026-05-20 23:05:17', '2026-05-26 20:35:44');
INSERT INTO `rent_order` VALUES (46, 'RENT1779289521340', 3, 32, '2026-05-20', '2026-05-21', NULL, 1, 343.00, 343.00, 'PENDING_PICKUP', 'PAID', NULL, NULL, '', '2026-05-20 23:05:21', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (47, 'RENT1779326473392', 3, 29, '2026-05-21', '2026-05-22', NULL, 1, 270.00, 270.00, 'PENDING_PICKUP', 'PAID', NULL, NULL, '', '2026-05-21 09:21:13', '2026-05-26 20:33:13');
INSERT INTO `rent_order` VALUES (48, 'RENT1779798216244', 3, 30, '2026-05-26', '2026-05-27', NULL, 1, 453.00, 453.00, 'CANCELLED', 'PAID', 'ALIPAY', '2026-05-26 20:23:36', '', '2026-05-26 20:23:36', '2026-05-26 20:23:36');
INSERT INTO `rent_order` VALUES (49, 'RENT1779798518374', 3, 30, '2026-05-26', '2026-05-28', NULL, 2, 453.00, 906.00, 'CANCELLED', 'UNPAID', NULL, NULL, '', '2026-05-26 20:28:38', '2026-05-26 20:28:38');
INSERT INTO `rent_order` VALUES (50, 'RENT1779798851121', 3, 26, '2026-05-26', '2026-05-27', NULL, 1, 300.00, 300.00, 'PENDING_PICKUP', 'UNPAID', NULL, NULL, '', '2026-05-26 20:34:11', '2026-05-26 20:34:11');
INSERT INTO `rent_order` VALUES (51, 'RENT1779799251761', 3, 30, '2026-05-26', '2026-05-27', NULL, 1, 453.00, 453.00, 'CANCELLED', 'UNPAID', NULL, NULL, '', '2026-05-26 20:40:51', '2026-05-26 20:40:51');
INSERT INTO `rent_order` VALUES (52, 'RENT1781532868700', 3, 27, '2026-06-15', '2026-06-16', NULL, 1, 220.00, 220.00, 'PENDING_PICKUP', 'PAID', 'ALIPAY', '2026-06-15 22:18:35', '', '2026-06-15 22:14:28', '2026-06-15 22:14:28');
INSERT INTO `rent_order` VALUES (53, 'RENT1781533125846', 3, 25, '2026-06-15', '2026-06-16', NULL, 1, 288.00, 288.00, 'PENDING_PICKUP', 'PAID', 'ALIPAY', '2026-06-15 22:18:49', '', '2026-06-15 22:18:45', '2026-06-15 22:18:45');

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
  `extra_fee_payment_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'UNPAID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING',
  `operator_id` bigint(0) NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `rent_order_id`(`rent_order_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of return_order
-- ----------------------------
INSERT INTO `return_order` VALUES (1, 2, '2026-03-09 11:26:49', 2, '', 0.00, 'UNPAID', 'CONFIRMED', 1, '2026-03-09 11:26:48', '2026-03-09 11:26:48');
INSERT INTO `return_order` VALUES (2, 3, '2026-03-09 11:38:28', 500, '', 0.00, 'UNPAID', 'CONFIRMED', 1, '2026-03-09 11:38:27', '2026-03-09 11:38:27');
INSERT INTO `return_order` VALUES (3, 4, '2026-03-09 11:50:11', 2, '没损坏', 0.00, 'UNPAID', 'CONFIRMED', 1, '2026-03-09 11:50:11', '2026-03-09 11:50:11');
INSERT INTO `return_order` VALUES (4, 5, '2026-03-14 11:42:44', 8, '', 0.00, 'UNPAID', 'CONFIRMED', 1, '2026-03-14 11:42:44', '2026-03-23 14:36:37');
INSERT INTO `return_order` VALUES (5, 6, '2026-03-23 10:42:45', 2, '', 0.00, 'UNPAID', 'CONFIRMED', 1, '2026-03-23 10:42:44', '2026-03-23 10:42:44');
INSERT INTO `return_order` VALUES (6, 8, '2026-03-23 14:24:04', 0, '', 0.00, 'UNPAID', 'CONFIRMED', 1, '2026-03-23 14:24:03', '2026-03-23 14:24:03');
INSERT INTO `return_order` VALUES (7, 7, '2026-03-23 14:24:32', 0, '', 0.00, 'UNPAID', 'CONFIRMED', 1, '2026-03-23 14:24:32', '2026-03-23 14:24:32');
INSERT INTO `return_order` VALUES (8, 11, '2026-03-18 18:40:00', 120, '', 0.00, 'UNPAID', 'CONFIRMED', 1, '2026-03-18 18:40:00', '2026-03-18 18:40:00');
INSERT INTO `return_order` VALUES (9, 12, '2026-03-19 14:20:00', 180, '', 0.00, 'UNPAID', 'CONFIRMED', 1, '2026-03-19 14:20:00', '2026-03-19 14:20:00');
INSERT INTO `return_order` VALUES (10, 13, '2026-03-11 10:10:00', 90, '', 0.00, 'UNPAID', 'CONFIRMED', 1, '2026-03-11 10:10:00', '2026-03-11 10:10:00');
INSERT INTO `return_order` VALUES (11, 14, '2026-02-15 17:30:00', 200, '', 0.00, 'UNPAID', 'CONFIRMED', 1, '2026-02-15 17:30:00', '2026-02-15 17:30:00');
INSERT INTO `return_order` VALUES (12, 15, '2026-01-07 12:15:00', 150, '', 0.00, 'UNPAID', 'CONFIRMED', 1, '2026-01-07 12:15:00', '2026-01-07 12:15:00');
INSERT INTO `return_order` VALUES (13, 16, '2025-12-22 19:40:00', 110, '', 0.00, 'UNPAID', 'CONFIRMED', 1, '2025-12-22 19:40:00', '2025-12-22 19:40:00');
INSERT INTO `return_order` VALUES (14, 17, '2024-07-10 16:10:00', 130, '', 0.00, 'UNPAID', 'CONFIRMED', 1, '2024-07-10 16:10:00', '2024-07-10 16:10:00');
INSERT INTO `return_order` VALUES (15, 18, '2023-03-18 12:25:00', 160, '', 0.00, 'UNPAID', 'CONFIRMED', 1, '2023-03-18 12:25:00', '2023-03-18 12:25:00');
INSERT INTO `return_order` VALUES (16, 19, '2022-01-14 10:10:00', 100, '', 0.00, 'UNPAID', 'CONFIRMED', 1, '2022-01-14 10:10:00', '2022-01-14 10:10:00');
INSERT INTO `return_order` VALUES (17, 20, '2026-04-21 14:11:58', 8, '', 0.00, 'UNPAID', 'CONFIRMED', 1, '2026-04-21 14:11:57', '2026-04-21 14:11:57');
INSERT INTO `return_order` VALUES (18, 21, '2026-05-02 23:09:41', 12100, '', 0.00, 'UNPAID', 'CONFIRMED', 1, '2026-05-02 23:09:40', '2026-05-02 23:09:40');
INSERT INTO `return_order` VALUES (19, 22, '2026-05-03 20:18:55', 7810, '后视镜坏了', 98.00, 'PAID', 'CONFIRMED', 1, '2026-05-03 20:18:55', '2026-05-26 20:52:00');
INSERT INTO `return_order` VALUES (20, 23, '2026-05-03 20:31:45', 6200, '', 12.00, 'PAID', 'CONFIRMED', 1, '2026-05-03 20:31:44', '2026-05-26 20:52:00');
INSERT INTO `return_order` VALUES (21, 25, '2026-05-09 15:07:58', 6233, '', 1.00, 'PAID', 'CONFIRMED', 1, '2026-05-09 15:07:57', '2026-05-26 20:52:00');
INSERT INTO `return_order` VALUES (22, 26, '2026-05-09 22:52:26', 7809, '', 1.00, 'PAID', 'CONFIRMED', 1, '2026-05-09 22:52:25', '2026-05-26 20:52:00');
INSERT INTO `return_order` VALUES (23, 37, '2026-05-16 14:37:47', 6241, '', 1.00, 'PAID', 'CONFIRMED', 1, '2026-05-16 14:37:46', '2026-05-26 20:52:00');
INSERT INTO `return_order` VALUES (24, 28, '2026-05-16 19:17:50', 4678, '无', 2.00, 'PAID', 'CONFIRMED', 1, '2026-05-16 19:17:50', '2026-05-26 20:52:00');
INSERT INTO `return_order` VALUES (25, 27, '2026-05-20 23:06:28', 11234, '', 9.00, 'PAID', 'CONFIRMED', 1, '2026-05-20 23:06:28', '2026-05-20 23:06:28');
INSERT INTO `return_order` VALUES (26, 45, '2026-05-26 20:49:57', 2343, '', 0.00, 'UNPAID', 'PENDING', NULL, '2026-05-26 20:49:57', '2026-05-26 20:49:57');

-- ----------------------------
-- Table structure for support_conversation
-- ----------------------------
DROP TABLE IF EXISTS `support_conversation`;
CREATE TABLE `support_conversation`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'OPEN',
  `assigned_admin_id` bigint(0) NULL DEFAULT NULL,
  `source_biz_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `source_biz_id` bigint(0) NULL DEFAULT NULL,
  `last_message_preview` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `last_message_time` datetime(0) NULL DEFAULT NULL,
  `user_unread_count` int(0) NOT NULL DEFAULT 0,
  `admin_unread_count` int(0) NOT NULL DEFAULT 0,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_support_conversation_user_status`(`user_id`, `status`) USING BTREE,
  INDEX `idx_support_conversation_status_time`(`status`, `last_message_time`, `id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of support_conversation
-- ----------------------------
INSERT INTO `support_conversation` VALUES (1, 3, 'CLOSED', 1, NULL, NULL, NULL, NULL, 0, 0, '2026-05-16 17:11:52', '2026-05-16 17:11:52');
INSERT INTO `support_conversation` VALUES (2, 3, 'CLOSED', 1, NULL, NULL, '你好', '2026-05-19 21:09:18', 0, 0, '2026-05-16 17:14:35', '2026-05-16 19:44:49');
INSERT INTO `support_conversation` VALUES (3, 3, 'CLOSED', 1, NULL, NULL, NULL, NULL, 0, 0, '2026-05-16 19:41:52', '2026-05-16 19:41:52');
INSERT INTO `support_conversation` VALUES (4, 3, 'CLOSED', 1, NULL, NULL, '你好', '2026-05-19 21:21:24', 0, 0, '2026-05-19 21:21:21', '2026-05-19 21:21:21');
INSERT INTO `support_conversation` VALUES (5, 3, 'OPEN', 1, NULL, NULL, '请问有什么可以帮到您吗', '2026-06-06 21:28:31', 0, 0, '2026-05-19 21:21:53', '2026-05-19 21:21:53');

-- ----------------------------
-- Table structure for support_message
-- ----------------------------
DROP TABLE IF EXISTS `support_message`;
CREATE TABLE `support_message`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `conversation_id` bigint(0) NOT NULL,
  `sender_id` bigint(0) NOT NULL,
  `sender_role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `read_status` tinyint(0) NOT NULL DEFAULT 0,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_support_message_conversation`(`conversation_id`, `id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 50 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of support_message
-- ----------------------------
INSERT INTO `support_message` VALUES (1, 2, 3, 'USER', '你好', 0, '2026-05-16 17:22:30', '2026-05-16 17:22:30');
INSERT INTO `support_message` VALUES (2, 2, 1, 'ADMIN', '您好，请问有什么问题吗', 0, '2026-05-16 17:22:50', '2026-05-16 17:22:50');
INSERT INTO `support_message` VALUES (3, 2, 3, 'USER', '哒哒哒哒哒，好想玩原神', 0, '2026-05-16 17:23:44', '2026-05-16 17:23:44');
INSERT INTO `support_message` VALUES (4, 2, 1, 'ADMIN', '原神？', 0, '2026-05-16 17:23:53', '2026-05-16 17:23:53');
INSERT INTO `support_message` VALUES (5, 2, 3, 'USER', '对啊，就是一个开放世界游戏', 0, '2026-05-16 17:24:15', '2026-05-16 17:24:15');
INSERT INTO `support_message` VALUES (6, 2, 1, 'ADMIN', '请问您对我们的车辆和系统有什么疑问吗？', 0, '2026-05-16 17:25:07', '2026-05-16 17:25:07');
INSERT INTO `support_message` VALUES (7, 2, 3, 'USER', '我想问小米yu7什么时候能租', 0, '2026-05-16 17:26:17', '2026-05-16 17:26:17');
INSERT INTO `support_message` VALUES (8, 2, 1, 'ADMIN', '您稍等，我帮您查一下', 0, '2026-05-16 17:26:33', '2026-05-16 17:26:33');
INSERT INTO `support_message` VALUES (9, 2, 1, 'ADMIN', '小米yu7现在就有车', 0, '2026-05-16 17:26:51', '2026-05-16 17:26:51');
INSERT INTO `support_message` VALUES (10, 2, 1, 'ADMIN', '原神是什么游戏', 0, '2026-05-16 17:57:55', '2026-05-16 17:57:55');
INSERT INTO `support_message` VALUES (11, 2, 1, 'ADMIN', '原神', 0, '2026-05-16 18:12:58', '2026-05-16 18:12:58');
INSERT INTO `support_message` VALUES (12, 2, 3, 'USER', '你好', 0, '2026-05-16 18:13:49', '2026-05-16 18:13:49');
INSERT INTO `support_message` VALUES (13, 2, 3, 'USER', '你好', 0, '2026-05-16 18:14:03', '2026-05-16 18:14:03');
INSERT INTO `support_message` VALUES (14, 2, 3, 'USER', '你好', 0, '2026-05-16 18:14:18', '2026-05-16 18:14:18');
INSERT INTO `support_message` VALUES (15, 2, 3, 'USER', '原神\n你好', 0, '2026-05-16 18:14:25', '2026-05-16 18:14:25');
INSERT INTO `support_message` VALUES (16, 2, 1, 'ADMIN', '你好', 0, '2026-05-16 18:16:09', '2026-05-16 18:16:09');
INSERT INTO `support_message` VALUES (17, 2, 1, 'ADMIN', 'hi', 0, '2026-05-16 18:32:21', '2026-05-16 18:32:21');
INSERT INTO `support_message` VALUES (18, 2, 3, 'USER', '你好', 0, '2026-05-16 18:34:40', '2026-05-16 18:34:40');
INSERT INTO `support_message` VALUES (19, 2, 3, 'USER', '你好啊哈哈哈哈', 0, '2026-05-16 18:53:21', '2026-05-16 18:53:21');
INSERT INTO `support_message` VALUES (20, 2, 1, 'ADMIN', '哈喽', 0, '2026-05-16 18:53:41', '2026-05-16 18:53:41');
INSERT INTO `support_message` VALUES (21, 2, 1, 'ADMIN', '哈喽', 0, '2026-05-16 18:53:53', '2026-05-16 18:53:53');
INSERT INTO `support_message` VALUES (22, 2, 3, 'USER', '你好', 0, '2026-05-16 18:54:04', '2026-05-16 18:54:04');
INSERT INTO `support_message` VALUES (23, 2, 3, 'USER', '你好', 0, '2026-05-16 18:54:15', '2026-05-16 18:54:15');
INSERT INTO `support_message` VALUES (24, 2, 1, 'ADMIN', '你好', 0, '2026-05-16 18:55:22', '2026-05-16 18:55:22');
INSERT INTO `support_message` VALUES (25, 2, 1, 'ADMIN', '你好', 0, '2026-05-16 18:55:31', '2026-05-16 18:55:31');
INSERT INTO `support_message` VALUES (26, 2, 3, 'USER', '你好', 0, '2026-05-16 18:56:56', '2026-05-16 18:56:56');
INSERT INTO `support_message` VALUES (27, 2, 1, 'ADMIN', '你好', 0, '2026-05-16 18:57:08', '2026-05-16 18:57:08');
INSERT INTO `support_message` VALUES (28, 2, 1, 'ADMIN', '你好', 0, '2026-05-16 18:58:15', '2026-05-16 18:58:15');
INSERT INTO `support_message` VALUES (29, 2, 1, 'ADMIN', '你好啊', 0, '2026-05-16 19:01:31', '2026-05-16 19:01:31');
INSERT INTO `support_message` VALUES (30, 2, 3, 'USER', '你好', 0, '2026-05-16 19:02:46', '2026-05-16 19:02:46');
INSERT INTO `support_message` VALUES (31, 2, 3, 'USER', 'hi', 0, '2026-05-16 19:08:34', '2026-05-16 19:08:34');
INSERT INTO `support_message` VALUES (32, 2, 3, 'USER', '你好', 0, '2026-05-16 19:08:44', '2026-05-16 19:08:44');
INSERT INTO `support_message` VALUES (33, 2, 1, 'ADMIN', '你好啊', 0, '2026-05-16 19:08:57', '2026-05-16 19:08:57');
INSERT INTO `support_message` VALUES (34, 2, 1, 'ADMIN', '你好', 0, '2026-05-16 19:34:15', '2026-05-16 19:34:15');
INSERT INTO `support_message` VALUES (37, 2, 3, 'USER', '哈哈哈哈哈哈哈', 0, '2026-05-16 19:35:16', '2026-05-16 19:35:16');
INSERT INTO `support_message` VALUES (38, 2, 3, 'USER', '好诡异', 0, '2026-05-16 19:35:20', '2026-05-16 19:35:20');
INSERT INTO `support_message` VALUES (39, 2, 1, 'ADMIN', '😎', 0, '2026-05-16 19:35:44', '2026-05-16 19:35:44');
INSERT INTO `support_message` VALUES (40, 2, 3, 'USER', '你好', 0, '2026-05-16 22:38:07', '2026-05-16 22:38:07');
INSERT INTO `support_message` VALUES (41, 2, 1, 'ADMIN', 'hi', 0, '2026-05-16 22:38:20', '2026-05-16 22:38:20');
INSERT INTO `support_message` VALUES (42, 2, 3, 'USER', '我的yu7没电了怎么办', 0, '2026-05-18 14:11:36', '2026-05-18 14:11:36');
INSERT INTO `support_message` VALUES (43, 2, 1, 'ADMIN', '你可以去充电', 0, '2026-05-18 14:11:48', '2026-05-18 14:11:48');
INSERT INTO `support_message` VALUES (44, 2, 3, 'USER', '怎么充电？', 0, '2026-05-18 14:12:00', '2026-05-18 14:12:00');
INSERT INTO `support_message` VALUES (45, 2, 1, 'ADMIN', '到充电桩插上充电枪然后扫码充电', 0, '2026-05-18 14:12:18', '2026-05-18 14:12:18');
INSERT INTO `support_message` VALUES (46, 2, 3, 'USER', 'wow', 0, '2026-05-18 14:12:25', '2026-05-18 14:12:25');
INSERT INTO `support_message` VALUES (47, 2, 1, 'ADMIN', '你好', 0, '2026-05-19 21:09:17', '2026-05-19 21:09:17');
INSERT INTO `support_message` VALUES (48, 4, 3, 'USER', '你好', 0, '2026-05-19 21:21:24', '2026-05-19 21:21:24');
INSERT INTO `support_message` VALUES (49, 5, 3, 'USER', '你好，我想咨询一下订单', 0, '2026-06-06 21:28:09', '2026-06-06 21:28:09');
INSERT INTO `support_message` VALUES (50, 5, 1, 'ADMIN', '请问有什么可以帮到您吗', 0, '2026-06-06 21:28:31', '2026-06-06 21:28:31');

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '21232f297a57a5a743894a0e4a801fc3', '系统管理员', '13800000001', '', '男', 'ADMIN', 1, '2026-03-08 23:12:23', '2026-03-08 23:12:23');
INSERT INTO `sys_user` VALUES (2, 'zhangsan', 'e10adc3949ba59abbe56e057f20f883e', '张三', '13900000001', '370101200101010002', '男', 'USER', 1, '2026-03-08 23:12:23', '2026-03-08 23:12:23');
INSERT INTO `sys_user` VALUES (3, 'lisi', 'e10adc3949ba59abbe56e057f20f883e', '李四', '13900000002', '370101200102020003', '女', 'USER', 1, '2026-03-08 23:12:23', '2026-03-08 23:12:23');
INSERT INTO `sys_user` VALUES (5, 'lzhgod1', '70b4f6d1610f4b4031e33ec0b4005b00', 'lzhgod', '13280761078', '', '', 'USER', 1, '2026-04-21 21:13:31', '2026-04-21 21:13:31');
INSERT INTO `sys_user` VALUES (6, 'lzhgod2', '70b4f6d1610f4b4031e33ec0b4005b00', 'lzhgod', '15622223333', '378324200011241428', '女', 'USER', 1, '2026-04-21 21:15:19', '2026-04-21 21:15:19');

-- ----------------------------
-- Table structure for user_favorite
-- ----------------------------
DROP TABLE IF EXISTS `user_favorite`;
CREATE TABLE `user_favorite`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `car_id` bigint(0) NOT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_car`(`user_id`, `car_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_favorite
-- ----------------------------
INSERT INTO `user_favorite` VALUES (6, 7, 4, '2026-05-10 19:24:48');
INSERT INTO `user_favorite` VALUES (7, 4, 2, '2026-05-11 16:25:23');
INSERT INTO `user_favorite` VALUES (10, 3, 24, '2026-05-12 22:33:58');
INSERT INTO `user_favorite` VALUES (11, 8, 23, '2026-05-12 22:48:05');
INSERT INTO `user_favorite` VALUES (12, 3, 25, '2026-05-14 20:25:39');
INSERT INTO `user_favorite` VALUES (16, 3, 18, '2026-05-15 13:04:13');
INSERT INTO `user_favorite` VALUES (20, 4, 1, '2026-05-15 15:40:17');
INSERT INTO `user_favorite` VALUES (25, 3, 22, '2026-05-15 15:53:18');
INSERT INTO `user_favorite` VALUES (26, 3, 26, '2026-05-15 15:56:17');
INSERT INTO `user_favorite` VALUES (27, 3, 16, '2026-05-15 15:56:41');
INSERT INTO `user_favorite` VALUES (28, 3, 15, '2026-05-15 15:56:41');
INSERT INTO `user_favorite` VALUES (29, 3, 3, '2026-05-15 15:56:43');
INSERT INTO `user_favorite` VALUES (30, 3, 1, '2026-05-15 15:56:44');
INSERT INTO `user_favorite` VALUES (31, 3, 28, '2026-05-15 16:55:39');
INSERT INTO `user_favorite` VALUES (32, 3, 27, '2026-05-15 16:55:41');
INSERT INTO `user_favorite` VALUES (33, 3, 30, '2026-05-15 17:38:15');
INSERT INTO `user_favorite` VALUES (34, 3, 29, '2026-05-15 17:38:15');
INSERT INTO `user_favorite` VALUES (35, 3, 32, '2026-05-16 14:54:04');
INSERT INTO `user_favorite` VALUES (36, 3, 17, '2026-05-16 15:17:29');
INSERT INTO `user_favorite` VALUES (37, 3, 33, '2026-05-16 16:30:04');
INSERT INTO `user_favorite` VALUES (38, 3, 4, '2026-05-16 16:30:09');

SET FOREIGN_KEY_CHECKS = 1;
