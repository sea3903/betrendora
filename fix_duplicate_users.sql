-- ═══════════════════════════════════════════════════════════════
-- FIX: Xóa user trùng lặp và thêm UNIQUE constraint
-- ═══════════════════════════════════════════════════════════════

-- Bước 1: Kiểm tra user trùng facebook_account_id
SELECT facebook_account_id, COUNT(*) as cnt 
FROM users 
WHERE facebook_account_id IS NOT NULL AND facebook_account_id != ''
GROUP BY facebook_account_id 
HAVING cnt > 1;

-- Bước 2: Xem chi tiết các user trùng (chạy sau khi xem kết quả bước 1)
-- SELECT * FROM users WHERE facebook_account_id = 'ID_TRÙNG_Ở_ĐÂY';

-- Bước 3: Xóa user cũ hơn (giữ lại user mới nhất) - LƯU Ý: Cần backup trước
-- DELETE FROM users WHERE facebook_account_id = 'ID_TRÙNG' AND id != (ID_GIỮ_LẠI);

-- Bước 4: Thêm UNIQUE constraint để ngăn trùng lặp trong tương lai
-- ALTER TABLE users ADD UNIQUE INDEX uk_facebook_account_id (facebook_account_id);
-- ALTER TABLE users ADD UNIQUE INDEX uk_google_account_id (google_account_id);

-- Bước 5: Mở rộng cột profile_image
ALTER TABLE users MODIFY COLUMN profile_image VARCHAR(500) DEFAULT '';
