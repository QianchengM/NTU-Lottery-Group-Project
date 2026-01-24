-- ================================
-- Minimal schema patch for new MVC features
-- ================================

-- 1) Idempotent invite submission: a user can bind an inviter only once.
--    You can run this on MySQL: lottery_db
-- =========================================================
-- NTU Lottery Project - Schema + Seed Data (MySQL)
-- Database: ntu_big_market
-- =========================================================

CREATE DATABASE IF NOT EXISTS ntu_big_market
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE ntu_big_market;

-- ---------- 0) Optional: drop tables (only if you want a clean reset) ----------
-- DROP TABLE IF EXISTS user_invite;
-- DROP TABLE IF EXISTS record;
-- DROP TABLE IF EXISTS prize;
-- DROP TABLE IF EXISTS sys_user;
-- DROP TABLE IF EXISTS activity;

-- ---------- 1) Activity (optional but recommended) ----------
-- 你代码里没有直接用 activity 表，但 prize/activity_id、record/activity_id 会更清晰
CREATE TABLE IF NOT EXISTS activity (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        name VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1, -- 1: active, 0: inactive
    start_time DATETIME NULL,
    end_time DATETIME NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 2) Users ----------
-- 对应代码：sys_user(id, username, points, last_checkin_date, invite_code)
CREATE TABLE IF NOT EXISTS sys_user (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        username VARCHAR(64) NOT NULL,
    points INT NOT NULL DEFAULT 0,
    last_checkin_date DATE NULL,
    invite_code VARCHAR(32) NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_user_invite_code (invite_code),
    KEY idx_sys_user_username (username)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 3) Prizes ----------
-- 对应代码：prize(id, name, stock, probability, type, activity_id, point_cost)
CREATE TABLE IF NOT EXISTS prize (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                     activity_id BIGINT NOT NULL,
                                     name VARCHAR(128) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    probability INT NOT NULL DEFAULT 0, -- 0~100，代码里默认按 100 计算
    type TINYINT NOT NULL DEFAULT 0,     -- 0:谢谢惠顾, 1:实物, 2:虚拟券
    point_cost INT NOT NULL DEFAULT 0,   -- 兑换商城用（>0 才会在 Mall 页面出现）
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_prize_activity (activity_id),
    KEY idx_prize_type (type)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 4) Records ----------
-- 对应代码：record(user_id, activity_id, prize_name, prize_type, create_time)
CREATE TABLE IF NOT EXISTS record (
                                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                      user_id BIGINT NOT NULL,
                                      activity_id BIGINT NOT NULL,
                                      prize_name VARCHAR(128) NOT NULL,
    prize_type TINYINT NOT NULL DEFAULT 0, -- 1 才会上榜 leaderboard
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_record_user (user_id, create_time),
    KEY idx_record_activity (activity_id, create_time),
    KEY idx_record_prize_type (prize_type, create_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 5) User Invite ----------
-- 对应 schema_patch.sql：user_invite(user_id unique, inviter_id)
CREATE TABLE IF NOT EXISTS user_invite (
                                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                           user_id BIGINT NOT NULL,
                                           inviter_id BIGINT NOT NULL,
                                           create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           UNIQUE KEY uk_user_invite_user_id (user_id),
    KEY idx_user_invite_inviter_id (inviter_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- Seed Data (safe re-run: truncate then insert)
-- =========================================================
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE user_invite;
TRUNCATE TABLE record;
TRUNCATE TABLE prize;
TRUNCATE TABLE sys_user;
TRUNCATE TABLE activity;
SET FOREIGN_KEY_CHECKS = 1;

-- 1) Activity
INSERT INTO activity (id, name, status, start_time, end_time)
VALUES
    (1, 'NTU Lottery Demo Activity', 1, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY));

-- 2) Users
INSERT INTO sys_user (id, username, points, last_checkin_date, invite_code)
VALUES
    (1, 'alice',   200, NULL, 'ALICE123'),
    (2, 'bob',     120, NULL, 'BOB456'),
    (3, 'charlie',  30, NULL, 'CHARLIE789');

-- 3) Invite relation (bob was invited by alice)
INSERT INTO user_invite (user_id, inviter_id)
VALUES
    (2, 1);

-- 4) Prizes for activity_id = 1
-- probability 总和建议=100（你代码是 nextInt(100)+1）
INSERT INTO prize (id, activity_id, name, stock, probability, type, point_cost)
VALUES
    (1, 1, '谢谢参与',        999999, 60, 0, 0),
    (2, 1, 'iPad mini',            2,  1, 1, 2500),
    (3, 1, 'NTU Hoodie',          10,  4, 1, 500),
    (4, 1, 'Coffee Voucher',     200, 15, 2, 50),
    (5, 1, '10 Points Coupon',   1000, 20, 2, 10);

-- 5) Some history records (for /api/user/history and /api/user/leaderboard)
INSERT INTO record (user_id, activity_id, prize_name, prize_type, create_time)
VALUES
    (1, 1, 'Coffee Voucher',     2, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (1, 1, 'NTU Hoodie',         1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2, 1, '10 Points Coupon',   2, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
    (3, 1, '谢谢参与',           0, DATE_SUB(NOW(), INTERVAL 1 HOUR));


-- 2) Ensure sys_user has required columns.
--    If you already have them, skip.

-- ALTER TABLE sys_user ADD COLUMN points INT NOT NULL DEFAULT 0;
-- ALTER TABLE sys_user ADD COLUMN last_checkin_date DATE NULL;
-- ALTER TABLE sys_user ADD COLUMN invite_code VARCHAR(32) NULL;
-- CREATE UNIQUE INDEX uk_sys_user_invite_code ON sys_user(invite_code);

-- 3) Ensure prize has point_cost for points-mall.
-- ALTER TABLE prize ADD COLUMN point_cost INT NOT NULL DEFAULT 0;
