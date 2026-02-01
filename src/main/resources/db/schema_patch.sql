CREATE DATABASE IF NOT EXISTS ntu_big_market
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE ntu_big_market;

-- ---------- 0) Optional: drop tables (only if you want a clean reset) ----------
DROP TABLE IF EXISTS user_invite;
DROP TABLE IF EXISTS record;
DROP TABLE IF EXISTS prize;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS activity;
DROP TABLE IF EXISTS sku;
DROP TABLE IF EXISTS activity_sku;
DROP TABLE IF EXISTS points_ledger;
DROP TABLE IF EXISTS task_message;
DROP TABLE IF EXISTS user_award_order;
DROP TABLE IF EXISTS user_take_order;

-- ---------- 1) Activity (optional but recommended) ----------
-- 你代码里没有直接用 activity 表，但 prize/activity_id、record/activity_id 会更清晰
CREATE TABLE IF NOT EXISTS activity (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        name VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1, -- 1: active, 0: inactive
    draw_cost INT NOT NULL DEFAULT 0, -- 每次抽奖消耗积分（可按活动配置）
    daily_draw_limit INT NOT NULL DEFAULT 0, -- 每人每天次数限制（0=不限）
    fallback_prize_id BIGINT NULL, -- 兜底奖品ID
    start_time DATETIME NULL,
    end_time DATETIME NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 2) Users ----------
-- 对应代码：sys_user(id, username, points, last_checkin_date, invite_code)
CREATE TABLE IF NOT EXISTS sys_user (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NULL,
    password_salt VARCHAR(64) NULL,
    points INT NOT NULL DEFAULT 0,
    level INT NOT NULL DEFAULT 0,
    last_checkin_date DATE NULL,
    invite_code VARCHAR(32) NULL,
    status TINYINT NOT NULL DEFAULT 1, -- 1: active, 0: disabled
    last_login_time DATETIME NULL,
    last_password_change DATETIME NULL,
    failed_login_count INT NOT NULL DEFAULT 0,
    locked_until DATETIME NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_user_invite_code (invite_code),
    KEY idx_sys_user_username (username)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 3) SKU ----------
CREATE TABLE IF NOT EXISTS sku (
                                   id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                   sku_code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sku_code (sku_code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 4) Activity SKU (stock by total/month/day) ----------
CREATE TABLE IF NOT EXISTS activity_sku (
                                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            activity_id BIGINT NOT NULL,
                                            sku_id BIGINT NOT NULL,
    stock_total INT NOT NULL DEFAULT 0,
    stock_month INT NOT NULL DEFAULT 0,
    stock_day INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_activity_sku (activity_id, sku_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 5) Prizes ----------
-- 对应代码：prize(id, name, sku_id, stock, probability, probability_decimal, type, activity_id, point_cost)
CREATE TABLE IF NOT EXISTS prize (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                     activity_id BIGINT NOT NULL,
                                     sku_id BIGINT NULL,
                                     name VARCHAR(128) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    probability INT NOT NULL DEFAULT 0, -- 兼容旧字段
    probability_decimal DECIMAL(20, 10) NULL, -- 高精度概率(0~1 或任意比例)
    type TINYINT NOT NULL DEFAULT 0,     -- 0:谢谢惠顾, 1:实物, 2:虚拟券
    point_cost INT NOT NULL DEFAULT 0,   -- 兑换商城用（>0 才会在 Mall 页面出现）
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_prize_activity (activity_id),
    KEY idx_prize_sku (sku_id),
    KEY idx_prize_type (type)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 6) Records ----------
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

-- ---------- 7) User Invite ----------
-- 对应 schema_patch.sql：user_invite(user_id unique, inviter_id)
CREATE TABLE IF NOT EXISTS user_invite (
                                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                           user_id BIGINT NOT NULL,
                                           inviter_id BIGINT NOT NULL,
                                           create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           UNIQUE KEY uk_user_invite_user_id (user_id),
    KEY idx_user_invite_inviter_id (inviter_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 8) Points Ledger (audit) ----------
-- 每次积分变动都记录一笔流水，便于对账/幂等/补偿
CREATE TABLE IF NOT EXISTS points_ledger (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                             user_id BIGINT NOT NULL,
                                             biz_type VARCHAR(32) NOT NULL,
    biz_id VARCHAR(64) NOT NULL,
    delta INT NOT NULL,
    balance_after INT NOT NULL,
    remark VARCHAR(255) NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_points_ledger_biz (biz_type, biz_id),
    KEY idx_points_ledger_user (user_id, create_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 9) Task Message (local outbox) ----------
-- Used for async reward dispatch & compensation (eventual consistency)
CREATE TABLE IF NOT EXISTS task_message (
                                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            biz_type VARCHAR(64) NOT NULL,
                                            biz_id VARCHAR(64) NOT NULL,
                                            topic VARCHAR(128) NOT NULL,
                                            payload JSON NOT NULL,
                                            state VARCHAR(16) NOT NULL,
                                            retry_count INT NOT NULL DEFAULT 0,
                                            next_retry_time DATETIME NOT NULL,
                                            last_error VARCHAR(512) NULL,
                                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                            UNIQUE KEY uk_task_biz (biz_type, biz_id),
                                            KEY idx_task_state_next (state, next_retry_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- ==============================
-- 10) user_take_order (事务A写入 processing；事务B更新 used)
-- ==============================
CREATE TABLE IF NOT EXISTS user_take_order (
                                               id           BIGINT PRIMARY KEY AUTO_INCREMENT,
                                               user_id      BIGINT NOT NULL,
                                               activity_id  BIGINT NOT NULL,
                                               biz_id       VARCHAR(64) NOT NULL,   -- 参与订单号（幂等键，建议用雪花/UUID）
    state        VARCHAR(16) NOT NULL,   -- PROCESSING / USED
    create_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_take_biz (biz_id),
    KEY idx_take_user_act (user_id, activity_id, create_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 11) user_award_order (事务B写入)
-- ==============================
CREATE TABLE IF NOT EXISTS user_award_order (
                                                id           BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                user_id      BIGINT NOT NULL,
                                                activity_id  BIGINT NOT NULL,
                                                take_biz_id  VARCHAR(64) NOT NULL,   -- 对应 user_take_order.biz_id（强幂等）
    prize_id     BIGINT NOT NULL,
    prize_name   VARCHAR(128) NOT NULL,
    prize_type   TINYINT NOT NULL DEFAULT 0,
    state        VARCHAR(16) NOT NULL,   -- CREATE / SENT
    create_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_award_take (take_biz_id),
    KEY idx_award_user (user_id, create_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- ==============================
-- 12) Activity Weight Rule
-- ==============================
CREATE TABLE IF NOT EXISTS activity_weight_rule (
                                                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                     activity_id BIGINT NOT NULL,
                                                     rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    rule_type VARCHAR(16) NOT NULL, -- POINTS / LEVEL
    min_value INT NOT NULL DEFAULT 0,
    max_value INT NOT NULL DEFAULT 2147483647,
    priority INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_weight_activity (activity_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 13) Activity Weight Prize
-- ==============================
CREATE TABLE IF NOT EXISTS activity_weight_prize (
                                                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                      rule_id BIGINT NOT NULL,
                                                      prize_id BIGINT NOT NULL,
                                                      weight INT NOT NULL DEFAULT 0,
                                                      KEY idx_weight_rule (rule_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 14) Risk Blacklist
-- ==============================
CREATE TABLE IF NOT EXISTS risk_blacklist (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              user_id BIGINT NOT NULL,
                                              reason VARCHAR(255) NULL,
                                              create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              UNIQUE KEY uk_blacklist_user (user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 15) Rebate Config
-- ==============================
CREATE TABLE IF NOT EXISTS rebate_config (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                             activity_id BIGINT NOT NULL,
                                             rebate_type VARCHAR(16) NOT NULL, -- POINTS
    rebate_value INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rebate_activity (activity_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 16) Rebate Order
-- ==============================
CREATE TABLE IF NOT EXISTS rebate_order (
                                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            user_id BIGINT NOT NULL,
                                            inviter_id BIGINT NOT NULL,
                                            activity_id BIGINT NOT NULL,
                                            rebate_type VARCHAR(16) NOT NULL,
                                            rebate_value INT NOT NULL,
                                            biz_id VARCHAR(64) NOT NULL,
                                            state VARCHAR(16) NOT NULL,
                                            create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            UNIQUE KEY uk_rebate_biz (biz_id),
    KEY idx_rebate_user (user_id, create_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================================================
-- Seed Data (safe re-run: truncate then insert)
-- =========================================================
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE user_invite;
TRUNCATE TABLE record;
TRUNCATE TABLE prize;
TRUNCATE TABLE activity_sku;
TRUNCATE TABLE sku;
TRUNCATE TABLE sys_user;
TRUNCATE TABLE activity;
TRUNCATE TABLE points_ledger;
TRUNCATE TABLE task_message;
TRUNCATE TABLE activity_weight_rule;
TRUNCATE TABLE activity_weight_prize;
TRUNCATE TABLE risk_blacklist;
TRUNCATE TABLE rebate_config;
TRUNCATE TABLE rebate_order;
SET FOREIGN_KEY_CHECKS = 1;

-- 1) Activity
INSERT INTO activity (id, name, status, draw_cost, daily_draw_limit, fallback_prize_id, start_time, end_time)
VALUES
    (1, 'NTU Lottery Demo Activity', 1, 10, 5, 1, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY));

-- 2) Users
INSERT INTO sys_user (id, username, password_hash, password_salt, points, level, last_checkin_date, invite_code, status, last_login_time, last_password_change, failed_login_count, locked_until)
VALUES
    (1, 'alice',   'Uv+NOHLSD2cqEUuineh97m21IdaLQIyT4hbVBX0Eo2Y=', 'Hz0WK0snZDaMC17f+fCW/A==', 200, 2, NULL, 'ALICE123', 1, NULL, NOW(), 0, NULL),
    (2, 'bob',     'HC0oc6xBtRwX/CTQQR2o8Ydlw/3LxkGpdG4C0bqC2jk=', 'Kh/dRHajdqgR8IIncxGN4w==', 120, 1, NULL, 'BOB456', 1, NULL, NOW(), 0, NULL),
    (3, 'charlie', 'j/+mAHv5ygHe8tMmv/qAciH1SPHKkyiathiMb/D3jR8=', 'Q9mvzg8LS5BfIFqvm0NOmg==', 30, 0, NULL, 'CHARLIE789', 1, NULL, NOW(), 0, NULL);

-- 3) Invite relation (bob was invited by alice)
INSERT INTO user_invite (user_id, inviter_id)
VALUES
    (2, 1);

-- 4) SKU
INSERT INTO sku (id, sku_code, name)
VALUES
    (1, 'SKU-THANKS', '谢谢参与'),
    (2, 'SKU-IPAD', 'iPad mini'),
    (3, 'SKU-HOODIE', 'NTU Hoodie'),
    (4, 'SKU-COFFEE', 'Coffee Voucher'),
    (5, 'SKU-POINTS', '10 Points Coupon');

-- 5) Activity SKU stocks (activity_id = 1)
INSERT INTO activity_sku (id, activity_id, sku_id, stock_total, stock_month, stock_day)
VALUES
    (1, 1, 1, 999999, 999999, 999999),
    (2, 1, 2, 2, 2, 2),
    (3, 1, 3, 10, 10, 10),
    (4, 1, 4, 200, 200, 200),
    (5, 1, 5, 1000, 1000, 1000);

-- 6) Prizes for activity_id = 1
-- probability_decimal 总和建议=1.0（高精度优先）
INSERT INTO prize (id, activity_id, sku_id, name, stock, probability, probability_decimal, type, point_cost)
VALUES
    (1, 1, 1, '谢谢参与',        999999, 60, 0.60, 0, 0),
    (2, 1, 2, 'iPad mini',            2,  1, 0.01, 1, 2500),
    (3, 1, 3, 'NTU Hoodie',          10,  4, 0.04, 1, 500),
    (4, 1, 4, 'Coffee Voucher',     200, 15, 0.15, 2, 50),
    (5, 1, 5, '10 Points Coupon',   1000, 20, 0.20, 2, 10);

-- 7) Weight rules (optional demo)
INSERT INTO activity_weight_rule (id, activity_id, rule_code, rule_name, rule_type, min_value, max_value, priority)
VALUES
    (1, 1, 'POINTS_HIGH', '高积分用户权重', 'POINTS', 150, 999999, 10);

INSERT INTO activity_weight_prize (id, rule_id, prize_id, weight)
VALUES
    (1, 1, 2, 3),
    (2, 1, 3, 8),
    (3, 1, 4, 20),
    (4, 1, 5, 30),
    (5, 1, 1, 39);

-- 8) Rebate config (optional demo)
INSERT INTO rebate_config (id, activity_id, rebate_type, rebate_value, status)
VALUES
    (1, 1, 'POINTS', 5, 1);

-- 9) Some history records (for /api/user/history and /api/user/leaderboard)
INSERT INTO record (user_id, activity_id, prize_name, prize_type, create_time)
VALUES
    (1, 1, 'Coffee Voucher',     2, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (1, 1, 'NTU Hoodie',         1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2, 1, '10 Points Coupon',   2, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
    (3, 1, '谢谢参与',           0, DATE_SUB(NOW(), INTERVAL 1 HOUR));
