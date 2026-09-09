-- 摄影约拍平台测试数据生成脚本
-- 为AI模型训练提供完整的测试数据

-- =====================================================
-- 1. 摄影师套餐数据 (photographer_package)
-- =====================================================

-- 清空现有数据（可选）
-- DELETE FROM photographer_package;

-- 插入套餐数据
INSERT INTO photographer_package (photographer_id, name, description, price, is_default, create_time, update_time) VALUES

-- 摄影师1 - 写真摄影专家
(1, '基础写真套餐', '包含2小时拍摄，提供20张精修照片，含化妆造型建议，适合个人写真', 299.00, 1, NOW(), NOW()),
(1, '高级写真套餐', '包含4小时拍摄，提供40张精修照片，含专业化妆造型，提供拍摄道具，适合艺术写真', 599.00, 0, NOW(), NOW()),
(1, '情侣写真套餐', '包含3小时拍摄，提供30张精修照片，含情侣化妆造型，提供浪漫场景布置', 499.00, 0, NOW(), NOW()),

-- 摄影师2 - 婚纱摄影专家
(2, '婚纱摄影基础版', '包含6小时拍摄，提供50张精修照片，含新娘化妆造型，提供婚纱建议，适合室内外拍摄', 1299.00, 1, NOW(), NOW()),
(2, '婚纱摄影豪华版', '包含8小时拍摄，提供80张精修照片，含专业化妆造型团队，提供多套婚纱，含外景拍摄', 2499.00, 0, NOW(), NOW()),
(2, '婚纱摄影定制版', '包含10小时拍摄，提供100张精修照片，含顶级化妆造型，提供定制婚纱，含多个外景地拍摄', 3999.00, 0, NOW(), NOW()),

-- 摄影师3 - 儿童摄影专家
(3, '儿童摄影套餐', '包含2小时拍摄，提供25张精修照片，含儿童化妆造型，提供可爱道具，适合3-12岁儿童', 399.00, 1, NOW(), NOW()),
(3, '亲子摄影套餐', '包含3小时拍摄，提供35张精修照片，含亲子化妆造型，提供温馨场景布置，适合全家福', 599.00, 0, NOW(), NOW()),
(3, '周岁纪念套餐', '包含4小时拍摄，提供45张精修照片，含专业化妆造型，提供周岁主题布置，含视频记录', 799.00, 0, NOW(), NOW()),

-- 摄影师4 - 商业摄影专家
(4, '商业摄影基础', '包含4小时拍摄，提供30张精修照片，适合产品展示、企业宣传等商业用途', 899.00, 1, NOW(), NOW()),
(4, '商业摄影专业', '包含6小时拍摄，提供50张精修照片，含专业灯光设备，适合高端商业摄影', 1499.00, 0, NOW(), NOW()),
(4, '活动摄影套餐', '包含8小时拍摄，提供80张精修照片，适合会议、活动、庆典等场合记录', 1999.00, 0, NOW(), NOW()),

-- 摄影师5 - 古风摄影专家
(5, '古风写真套餐', '包含3小时拍摄，提供30张精修照片，含古风化妆造型，提供古装道具，适合汉服写真', 699.00, 1, NOW(), NOW()),
(5, '古风婚纱套餐', '包含6小时拍摄，提供60张精修照片，含古风婚纱造型，提供古风场景布置', 1599.00, 0, NOW(), NOW()),
(5, '古风全家福', '包含4小时拍摄，提供40张精修照片，含全家古风造型，提供古风场景，适合全家福', 999.00, 0, NOW(), NOW()),

-- 摄影师6 - 时尚摄影专家
(6, '街拍写真套餐', '包含2小时拍摄，提供20张精修照片，含时尚化妆造型，适合街头时尚摄影', 399.00, 1, NOW(), NOW()),
(6, '时尚大片套餐', '包含4小时拍摄，提供40张精修照片，含专业化妆造型，提供时尚道具，适合时尚摄影', 799.00, 0, NOW(), NOW()),
(6, '杂志风格套餐', '包含6小时拍摄，提供60张精修照片，含顶级化妆造型，提供专业灯光，适合杂志风格', 1299.00, 0, NOW(), NOW()),

-- 摄影师7 - 毕业摄影专家
(7, '毕业照套餐', '包含2小时拍摄，提供25张精修照片，含毕业妆造型，提供学士服，适合毕业纪念', 299.00, 1, NOW(), NOW()),
(7, '毕业写真套餐', '包含4小时拍摄，提供40张精修照片，含专业化妆造型，提供多个场景，含视频记录', 599.00, 0, NOW(), NOW()),
(7, '毕业团体套餐', '包含6小时拍摄，提供60张精修照片，适合班级团体毕业照，含个人和团体照片', 899.00, 0, NOW(), NOW());

-- =====================================================
-- 2. 订单数据 (order)
-- =====================================================

-- 插入订单数据
INSERT INTO `order` (user_id, photographer_id, package_id, order_no, status, total_amount, deposit_amount, shooting_date, shooting_location, requirements, create_time, update_time) VALUES

-- 用户1的订单
(1, 1, 1, 'ORD20240101001', 1, 299.00, 100.00, '2024-01-15', '室内摄影棚', '希望拍摄清新风格的个人写真', NOW(), NOW()),
(1, 2, 4, 'ORD20240101002', 2, 1299.00, 300.00, '2024-02-20', '海边外景', '婚纱照希望在海边拍摄，风格浪漫', NOW(), NOW()),

-- 用户2的订单
(2, 3, 7, 'ORD20240102001', 1, 399.00, 150.00, '2024-01-20', '儿童摄影棚', '为5岁女儿拍摄生日写真', NOW(), NOW()),
(2, 5, 13, 'ORD20240102002', 3, 699.00, 200.00, '2024-03-10', '古风摄影棚', '想拍摄汉服写真，风格古典优雅', NOW(), NOW()),

-- 用户3的订单
(3, 4, 10, 'ORD20240103001', 2, 899.00, 300.00, '2024-02-05', '公司会议室', '企业宣传照片拍摄', NOW(), NOW()),
(3, 6, 16, 'ORD20240103002', 1, 399.00, 100.00, '2024-01-25', '城市街头', '时尚街拍写真', NOW(), NOW()),

-- 用户4的订单
(4, 7, 19, 'ORD20240104001', 1, 299.00, 100.00, '2024-06-15', '大学校园', '毕业照拍摄', NOW(), NOW()),
(4, 2, 5, 'ORD20240104002', 2, 2499.00, 500.00, '2024-05-20', '教堂+外景', '豪华婚纱摄影套餐', NOW(), NOW()),

-- 用户5的订单
(5, 1, 3, 'ORD20240105001', 3, 499.00, 150.00, '2024-02-14', '浪漫咖啡厅', '情侣写真，希望风格温馨浪漫', NOW(), NOW()),
(5, 3, 9, 'ORD20240105002', 1, 799.00, 200.00, '2024-04-15', '周岁摄影棚', '宝宝周岁纪念照', NOW(), NOW());

-- =====================================================
-- 3. 作品数据 (portfolio)
-- =====================================================

-- 插入作品数据
INSERT INTO portfolio (photographer_id, title, description, category, tags, cover_image, image_urls, shooting_date, shooting_location, equipment, status, view_count, like_count, is_featured, sort_weight, create_time, update_time) VALUES

-- 摄影师1的作品
(1, '清新个人写真', '清新自然的个人写真作品，展现最真实的自己', '写真', '清新,自然,个人写真', '/images/portfolio/1_cover1.jpg', '["/images/portfolio/1_1.jpg","/images/portfolio/1_2.jpg","/images/portfolio/1_3.jpg"]', '2024-01-10', '室内摄影棚', 'Canon EOS R5, 24-70mm f/2.8', 1, 156, 23, 1, 100, NOW(), NOW()),
(1, '艺术写真系列', '富有艺术感的写真作品，展现独特的视觉风格', '写真', '艺术,创意,写真', '/images/portfolio/1_cover2.jpg', '["/images/portfolio/1_4.jpg","/images/portfolio/1_5.jpg","/images/portfolio/1_6.jpg"]', '2024-01-15', '艺术摄影棚', 'Canon EOS R5, 85mm f/1.4', 1, 89, 15, 0, 80, NOW(), NOW()),

-- 摄影师2的作品
(2, '海边婚纱照', '浪漫的海边婚纱摄影，记录最美的爱情时刻', '婚纱', '婚纱,海边,浪漫', '/images/portfolio/2_cover1.jpg', '["/images/portfolio/2_1.jpg","/images/portfolio/2_2.jpg","/images/portfolio/2_3.jpg"]', '2024-01-20', '海边外景', 'Sony A7R IV, 70-200mm f/2.8', 1, 234, 45, 1, 100, NOW(), NOW()),
(2, '教堂婚纱摄影', '庄重典雅的教堂婚纱摄影，神圣而美好', '婚纱', '婚纱,教堂,神圣', '/images/portfolio/2_cover2.jpg', '["/images/portfolio/2_4.jpg","/images/portfolio/2_5.jpg","/images/portfolio/2_6.jpg"]', '2024-01-25', '教堂', 'Sony A7R IV, 24-70mm f/2.8', 1, 178, 32, 0, 90, NOW(), NOW()),

-- 摄影师3的作品
(3, '儿童摄影作品', '纯真可爱的儿童摄影，记录童年的美好时光', '儿童', '儿童,可爱,童年', '/images/portfolio/3_cover1.jpg', '["/images/portfolio/3_1.jpg","/images/portfolio/3_2.jpg","/images/portfolio/3_3.jpg"]', '2024-01-12', '儿童摄影棚', 'Nikon Z6, 50mm f/1.8', 1, 145, 28, 1, 95, NOW(), NOW()),
(3, '亲子摄影系列', '温馨的亲子摄影，记录家庭的幸福时刻', '儿童', '亲子,温馨,家庭', '/images/portfolio/3_cover2.jpg', '["/images/portfolio/3_4.jpg","/images/portfolio/3_5.jpg","/images/portfolio/3_6.jpg"]', '2024-01-18', '家庭摄影棚', 'Nikon Z6, 35mm f/1.8', 1, 112, 19, 0, 85, NOW(), NOW()),

-- 摄影师4的作品
(4, '商业产品摄影', '专业的商业产品摄影，突出产品特色', '商业', '商业,产品,专业', '/images/portfolio/4_cover1.jpg', '["/images/portfolio/4_1.jpg","/images/portfolio/4_2.jpg","/images/portfolio/4_3.jpg"]', '2024-01-14', '商业摄影棚', 'Canon EOS R6, 100mm f/2.8', 1, 98, 12, 1, 90, NOW(), NOW()),
(4, '企业活动摄影', '企业活动记录摄影，展现企业文化', '商业', '企业,活动,文化', '/images/portfolio/4_cover2.jpg', '["/images/portfolio/4_4.jpg","/images/portfolio/4_5.jpg","/images/portfolio/4_6.jpg"]', '2024-01-22', '企业会议室', 'Canon EOS R6, 24-105mm f/4', 1, 76, 8, 0, 75, NOW(), NOW()),

-- 摄影师5的作品
(5, '古风汉服写真', '古典优雅的古风汉服写真，展现传统文化之美', '古风', '古风,汉服,古典', '/images/portfolio/5_cover1.jpg', '["/images/portfolio/5_1.jpg","/images/portfolio/5_2.jpg","/images/portfolio/5_3.jpg"]', '2024-01-16', '古风摄影棚', 'Fujifilm GFX 50S, 80mm f/1.7', 1, 189, 34, 1, 100, NOW(), NOW()),
(5, '古风婚纱摄影', '古风婚纱摄影，传统与现代的完美结合', '古风', '古风,婚纱,传统', '/images/portfolio/5_cover2.jpg', '["/images/portfolio/5_4.jpg","/images/portfolio/5_5.jpg","/images/portfolio/5_6.jpg"]', '2024-01-24', '古风外景', 'Fujifilm GFX 50S, 110mm f/2', 1, 156, 29, 0, 95, NOW(), NOW()),

-- 摄影师6的作品
(6, '时尚街拍写真', '时尚前卫的街拍写真，展现都市时尚魅力', '时尚', '时尚,街拍,都市', '/images/portfolio/6_cover1.jpg', '["/images/portfolio/6_1.jpg","/images/portfolio/6_2.jpg","/images/portfolio/6_3.jpg"]', '2024-01-13', '城市街头', 'Leica M10, 35mm f/1.4', 1, 167, 31, 1, 95, NOW(), NOW()),
(6, '杂志风格摄影', '杂志风格的专业摄影，展现时尚大片效果', '时尚', '时尚,杂志,专业', '/images/portfolio/6_cover2.jpg', '["/images/portfolio/6_4.jpg","/images/portfolio/6_5.jpg","/images/portfolio/6_6.jpg"]', '2024-01-19', '时尚摄影棚', 'Leica M10, 50mm f/1.4', 1, 134, 25, 0, 85, NOW(), NOW()),

-- 摄影师7的作品
(7, '毕业照作品', '青春活力的毕业照，记录学生时代的美好', '毕业', '毕业,青春,学生', '/images/portfolio/7_cover1.jpg', '["/images/portfolio/7_1.jpg","/images/portfolio/7_2.jpg","/images/portfolio/7_3.jpg"]', '2024-01-11', '大学校园', 'Canon EOS 90D, 18-55mm f/3.5-5.6', 1, 123, 18, 1, 90, NOW(), NOW()),
(7, '毕业团体照', '温馨的毕业团体照，记录同窗情谊', '毕业', '毕业,团体,同窗', '/images/portfolio/7_cover2.jpg', '["/images/portfolio/7_4.jpg","/images/portfolio/7_5.jpg","/images/portfolio/7_6.jpg"]', '2024-01-17', '大学校园', 'Canon EOS 90D, 24-70mm f/2.8', 1, 98, 14, 0, 80, NOW(), NOW());

-- =====================================================
-- 4. 查询验证数据
-- =====================================================

-- 查看套餐数据
SELECT 
    '套餐数据' as 数据类型,
    COUNT(*) as 数量
FROM photographer_package
UNION ALL
SELECT 
    '订单数据' as 数据类型,
    COUNT(*) as 数量
FROM `order`
UNION ALL
SELECT 
    '作品数据' as 数据类型,
    COUNT(*) as 数量
FROM portfolio;

-- 查看摄影师套餐详情
SELECT 
    p.id as photographer_id,
    p.name as photographer_name,
    pp.name as package_name,
    pp.description,
    pp.price,
    CASE pp.is_default WHEN 1 THEN '是' ELSE '否' END as is_default
FROM photographer_package pp
LEFT JOIN photographer p ON pp.photographer_id = p.id
ORDER BY pp.photographer_id, pp.is_default DESC, pp.price ASC; 