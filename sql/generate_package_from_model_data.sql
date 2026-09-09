-- 基于模型训练数据生成摄影师套餐数据
-- 根据 enhanced_train.jsonl 中的套餐信息生成

-- 清空现有套餐数据（可选）
-- DELETE FROM photographer_package;

-- 为7个摄影师生成对应的套餐数据

-- 摄影师1：写真摄影专家
INSERT INTO photographer_package (photographer_id, name, description, price, duration, photo_count, service_details, category, status, is_default, create_time, update_time) VALUES
(1, '基础写真套餐', '适合个人写真拍摄，风格清新自然', 299.00, 2, 20, '包含化妆造型建议、拍摄道具、浪漫场景布置', '写真摄影', 1, 1, NOW(), NOW()),
(1, '高级写真套餐', '专业写真拍摄，更多精修照片', 599.00, 4, 40, '包含专业化妆造型、高级道具、多场景拍摄', '写真摄影', 1, 0, NOW(), NOW()),
(1, '情侣写真套餐', '浪漫情侣写真，记录美好时光', 499.00, 3, 30, '包含情侣化妆造型、浪漫道具、温馨场景', '写真摄影', 1, 0, NOW(), NOW());

-- 摄影师2：婚纱摄影专家
INSERT INTO photographer_package (photographer_id, name, description, price, duration, photo_count, service_details, category, status, is_default, create_time, update_time) VALUES
(2, '婚纱摄影基础版', '包含6小时拍摄，提供50张精修照片，含新娘化妆造型，提供婚纱建议，适合室内外拍摄', 1299.00, 6, 50, '包含新娘化妆造型、婚纱建议、外景拍摄等服务', '婚纱摄影', 1, 1, NOW(), NOW()),
(2, '婚纱摄影豪华版', '包含8小时拍摄，提供80张精修照片，含专业化妆造型团队，提供多套婚纱，含外景拍摄', 2499.00, 8, 80, '包含专业化妆造型团队、多套婚纱、多个外景地拍摄', '婚纱摄影', 1, 0, NOW(), NOW()),
(2, '婚纱摄影定制版', '包含10小时拍摄，提供100张精修照片，含顶级化妆造型，提供定制婚纱，含多个外景地拍摄', 3999.00, 10, 100, '包含顶级化妆造型、定制婚纱、多个外景地拍摄', '婚纱摄影', 1, 0, NOW(), NOW());

-- 摄影师3：儿童摄影专家
INSERT INTO photographer_package (photographer_id, name, description, price, duration, photo_count, service_details, category, status, is_default, create_time, update_time) VALUES
(3, '儿童摄影套餐', '包含2小时拍摄，提供25张精修照片，含儿童化妆造型，提供可爱道具，适合3-12岁儿童', 399.00, 2, 25, '包含儿童化妆造型、可爱道具、温馨场景布置', '儿童摄影', 1, 1, NOW(), NOW()),
(3, '亲子摄影套餐', '包含3小时拍摄，提供35张精修照片，含亲子化妆造型，提供温馨场景布置，适合全家福', 599.00, 3, 35, '包含亲子化妆造型、温馨场景布置、适合全家福', '儿童摄影', 1, 0, NOW(), NOW()),
(3, '周岁纪念套餐', '包含4小时拍摄，提供45张精修照片，含专业化妆造型，提供周岁主题布置，含视频记录', 799.00, 4, 45, '包含专业化妆造型、周岁主题布置、含视频记录', '儿童摄影', 1, 0, NOW(), NOW());

-- 摄影师4：商业摄影专家
INSERT INTO photographer_package (photographer_id, name, description, price, duration, photo_count, service_details, category, status, is_default, create_time, update_time) VALUES
(4, '商业摄影基础', '包含4小时拍摄，提供30张精修照片，适合产品展示、企业宣传等商业用途', 899.00, 4, 30, '适合产品展示、企业宣传等商业用途', '商业摄影', 1, 1, NOW(), NOW()),
(4, '商业摄影专业', '包含6小时拍摄，提供50张精修照片，含专业灯光设备，适合高端商业摄影', 1499.00, 6, 50, '含专业灯光设备、高端商业摄影服务', '商业摄影', 1, 0, NOW(), NOW()),
(4, '活动摄影套餐', '包含8小时拍摄，提供80张精修照片，适合会议、活动、庆典等场合记录', 1999.00, 8, 80, '适合会议、活动、庆典等场合记录', '商业摄影', 1, 0, NOW(), NOW());

-- 摄影师5：古风摄影专家
INSERT INTO photographer_package (photographer_id, name, description, price, duration, photo_count, service_details, category, status, is_default, create_time, update_time) VALUES
(5, '古风写真套餐', '包含3小时拍摄，提供30张精修照片，含古风化妆造型，提供古装道具，适合汉服写真', 699.00, 3, 30, '提供古风化妆造型、古装道具、古风场景布置', '古风摄影', 1, 1, NOW(), NOW()),
(5, '古风婚纱套餐', '包含6小时拍摄，提供60张精修照片，含古风婚纱造型，提供古风场景布置', 1599.00, 6, 60, '含古风婚纱造型、古风场景布置', '古风摄影', 1, 0, NOW(), NOW()),
(5, '古风全家福', '包含4小时拍摄，提供40张精修照片，含全家古风造型，提供古风场景，适合全家福', 999.00, 4, 40, '含全家古风造型、古风场景、适合全家福', '古风摄影', 1, 0, NOW(), NOW());

-- 摄影师6：时尚摄影专家
INSERT INTO photographer_package (photographer_id, name, description, price, duration, photo_count, service_details, category, status, is_default, create_time, update_time) VALUES
(6, '街拍写真套餐', '包含2小时拍摄，提供20张精修照片，含时尚化妆造型，适合街头时尚摄影', 399.00, 2, 20, '提供时尚化妆造型、时尚道具、专业灯光', '时尚摄影', 1, 1, NOW(), NOW()),
(6, '时尚大片套餐', '包含4小时拍摄，提供40张精修照片，含专业化妆造型，提供时尚道具，适合时尚摄影', 799.00, 4, 40, '含专业化妆造型、时尚道具、专业灯光', '时尚摄影', 1, 0, NOW(), NOW()),
(6, '杂志风格套餐', '包含6小时拍摄，提供60张精修照片，含顶级化妆造型，提供专业灯光，适合杂志风格', 1299.00, 6, 60, '含顶级化妆造型、专业灯光、适合杂志风格', '时尚摄影', 1, 0, NOW(), NOW());

-- 摄影师7：毕业摄影专家
INSERT INTO photographer_package (photographer_id, name, description, price, duration, photo_count, service_details, category, status, is_default, create_time, update_time) VALUES
(7, '毕业照套餐', '包含2小时拍摄，提供25张精修照片，含毕业妆造型，提供学士服，适合毕业纪念', 299.00, 2, 25, '提供毕业妆造型、学士服、多个场景拍摄', '毕业摄影', 1, 1, NOW(), NOW()),
(7, '毕业写真套餐', '包含4小时拍摄，提供40张精修照片，含专业化妆造型，提供多个场景，含视频记录', 599.00, 4, 40, '含专业化妆造型、多个场景、含视频记录', '毕业摄影', 1, 0, NOW(), NOW()),
(7, '毕业团体套餐', '包含6小时拍摄，提供60张精修照片，适合班级团体毕业照，含个人和团体照片', 899.00, 6, 60, '适合班级团体毕业照、含个人和团体照片', '毕业摄影', 1, 0, NOW(), NOW());

-- 为每个摄影师添加通用套餐（确保每个摄影师都有基础套餐）
-- 基础写真套餐（所有摄影师都有）
INSERT INTO photographer_package (photographer_id, name, description, price, duration, photo_count, service_details, category, status, is_default, create_time, update_time) VALUES
(2, '基础写真套餐', '个人写真拍摄，清新自然风格', 299.00, 2, 20, '包含化妆造型建议、拍摄道具、基础场景', '写真摄影', 1, 0, NOW(), NOW()),
(3, '基础写真套餐', '个人写真拍摄，清新自然风格', 299.00, 2, 20, '包含化妆造型建议、拍摄道具、基础场景', '写真摄影', 1, 0, NOW(), NOW()),
(4, '基础写真套餐', '个人写真拍摄，清新自然风格', 299.00, 2, 20, '包含化妆造型建议、拍摄道具、基础场景', '写真摄影', 1, 0, NOW(), NOW()),
(5, '基础写真套餐', '个人写真拍摄，清新自然风格', 299.00, 2, 20, '包含化妆造型建议、拍摄道具、基础场景', '写真摄影', 1, 0, NOW(), NOW()),
(6, '基础写真套餐', '个人写真拍摄，清新自然风格', 299.00, 2, 20, '包含化妆造型建议、拍摄道具、基础场景', '写真摄影', 1, 0, NOW(), NOW()),
(7, '基础写真套餐', '个人写真拍摄，清新自然风格', 299.00, 2, 20, '包含化妆造型建议、拍摄道具、基础场景', '写真摄影', 1, 0, NOW(), NOW());

-- 高级写真套餐（所有摄影师都有）
INSERT INTO photographer_package (photographer_id, name, description, price, duration, photo_count, service_details, category, status, is_default, create_time, update_time) VALUES
(2, '高级写真套餐', '专业写真拍摄，更多精修照片', 599.00, 4, 40, '包含专业化妆造型、高级道具、多场景拍摄', '写真摄影', 1, 0, NOW(), NOW()),
(3, '高级写真套餐', '专业写真拍摄，更多精修照片', 599.00, 4, 40, '包含专业化妆造型、高级道具、多场景拍摄', '写真摄影', 1, 0, NOW(), NOW()),
(4, '高级写真套餐', '专业写真拍摄，更多精修照片', 599.00, 4, 40, '包含专业化妆造型、高级道具、多场景拍摄', '写真摄影', 1, 0, NOW(), NOW()),
(5, '高级写真套餐', '专业写真拍摄，更多精修照片', 599.00, 4, 40, '包含专业化妆造型、高级道具、多场景拍摄', '写真摄影', 1, 0, NOW(), NOW()),
(6, '高级写真套餐', '专业写真拍摄，更多精修照片', 599.00, 4, 40, '包含专业化妆造型、高级道具、多场景拍摄', '写真摄影', 1, 0, NOW(), NOW()),
(7, '高级写真套餐', '专业写真拍摄，更多精修照片', 599.00, 4, 40, '包含专业化妆造型、高级道具、多场景拍摄', '写真摄影', 1, 0, NOW(), NOW());

-- 情侣写真套餐（所有摄影师都有）
INSERT INTO photographer_package (photographer_id, name, description, price, duration, photo_count, service_details, category, status, is_default, create_time, update_time) VALUES
(2, '情侣写真套餐', '浪漫情侣写真，记录美好时光', 499.00, 3, 30, '包含情侣化妆造型、浪漫道具、温馨场景', '写真摄影', 1, 0, NOW(), NOW()),
(3, '情侣写真套餐', '浪漫情侣写真，记录美好时光', 499.00, 3, 30, '包含情侣化妆造型、浪漫道具、温馨场景', '写真摄影', 1, 0, NOW(), NOW()),
(4, '情侣写真套餐', '浪漫情侣写真，记录美好时光', 499.00, 3, 30, '包含情侣化妆造型、浪漫道具、温馨场景', '写真摄影', 1, 0, NOW(), NOW()),
(5, '情侣写真套餐', '浪漫情侣写真，记录美好时光', 499.00, 3, 30, '包含情侣化妆造型、浪漫道具、温馨场景', '写真摄影', 1, 0, NOW(), NOW()),
(6, '情侣写真套餐', '浪漫情侣写真，记录美好时光', 499.00, 3, 30, '包含情侣化妆造型、浪漫道具、温馨场景', '写真摄影', 1, 0, NOW(), NOW()),
(7, '情侣写真套餐', '浪漫情侣写真，记录美好时光', 499.00, 3, 30, '包含情侣化妆造型、浪漫道具、温馨场景', '写真摄影', 1, 0, NOW(), NOW());

-- 查询生成的套餐数据
SELECT
    p.id as photographer_id,
    p.name as photographer_name,
    pp.name as package_name,
    pp.price,
    pp.duration,
    pp.photo_count,
    pp.category
FROM photographer p
LEFT JOIN photographer_package pp ON p.id = pp.photographer_id
ORDER BY p.id, pp.price;

-- 统计每个摄影师的套餐数量
SELECT
    p.name as photographer_name,
    COUNT(pp.id) as package_count,
    MIN(pp.price) as min_price,
    MAX(pp.price) as max_price
FROM photographer p
LEFT JOIN photographer_package pp ON p.id = pp.photographer_id
GROUP BY p.id, p.name
ORDER BY p.id;

-- 按分类统计套餐数量
SELECT
    category,
    COUNT(*) as package_count,
    AVG(price) as avg_price,
    MIN(price) as min_price,
    MAX(price) as max_price
FROM photographer_package
GROUP BY category
ORDER BY package_count DESC; 