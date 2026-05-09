-- 订单状态迁移: RENTED → PENDING_PICKUP (已有的未还车订单视为租赁中)
UPDATE rent_order SET order_status = 'RENTED' WHERE order_status = 'RENTED';
-- 已归还的订单改为已完成
UPDATE rent_order SET order_status = 'COMPLETED' WHERE order_status = 'RETURNED';

-- 车辆状态迁移: 有活跃订单的车标为租赁中
UPDATE car_info SET status = 'RENTED'
WHERE id IN (SELECT car_id FROM rent_order WHERE order_status IN ('PENDING_PICKUP', 'RENTED', 'RETURN_PENDING'));

-- 有活跃工单的车标为对应状态
UPDATE car_info SET status = 'REPAIRING'
WHERE id IN (SELECT car_id FROM fault_report WHERE fault_status = 'REPAIRING')
  AND status NOT IN ('RENTED');

UPDATE car_info SET status = 'AWAITING_REPAIR'
WHERE id IN (SELECT car_id FROM fault_report WHERE fault_status = 'PENDING')
  AND status NOT IN ('RENTED', 'REPAIRING');

-- 其余为空闲
UPDATE car_info SET status = 'AVAILABLE'
WHERE status NOT IN ('RENTED', 'REPAIRING', 'AWAITING_REPAIR', 'DISABLED');
