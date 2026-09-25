MERGE INTO garage_zone (id, zone_code, zone_name, parent_id, level, sort_order, status) KEY(id) VALUES
(1, 'ZONE-A', 'A区车库', 0, 1, 1, 1),
(2, 'ZONE-B', 'B区车库', 0, 1, 2, 1),
(3, 'ZONE-C', 'C区车库', 0, 1, 3, 1);

MERGE INTO light_group (id, group_code, power, zone_id, installation_location, status) KEY(id) VALUES
(1, 'LG-A-001', 30, 1, 'A区-入口', 1),
(2, 'LG-A-002', 30, 1, 'A区-主通道', 1),
(3, 'LG-A-003', 25, 1, 'A区-车位', 1),
(4, 'LG-B-001', 30, 2, 'B区-入口', 1);
