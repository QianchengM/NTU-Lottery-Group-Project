-- ================================
-- Minimal schema patch for new MVC features
-- ================================

-- 1) Idempotent invite submission: a user can bind an inviter only once.
--    You can run this on MySQL: lottery_db

CREATE TABLE IF NOT EXISTS user_invite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  inviter_id BIGINT NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_invite_user_id (user_id),
  INDEX idx_user_invite_inviter_id (inviter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2) Ensure sys_user has required columns.
--    If you already have them, skip.

-- ALTER TABLE sys_user ADD COLUMN points INT NOT NULL DEFAULT 0;
-- ALTER TABLE sys_user ADD COLUMN last_checkin_date DATE NULL;
-- ALTER TABLE sys_user ADD COLUMN invite_code VARCHAR(32) NULL;
-- CREATE UNIQUE INDEX uk_sys_user_invite_code ON sys_user(invite_code);

-- 3) Ensure prize has point_cost for points-mall.
-- ALTER TABLE prize ADD COLUMN point_cost INT NOT NULL DEFAULT 0;
