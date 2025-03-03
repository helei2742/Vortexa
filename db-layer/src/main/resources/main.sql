/*
 Navicat Premium Data Transfer

 Source Server         : auto_bot
 Source Server Type    : SQLite
 Source Server Version : 3035005 (3.35.5)
 Source Schema         : main

 Target Server Type    : SQLite
 Target Server Version : 3035005 (3.35.5)
 File Encoding         : 65001

 Date: 03/03/2025 13:45:40
*/

PRAGMA foreign_keys = false;

-- ----------------------------
-- Table structure for _t_bot_account_context_6_old_20250215
-- ----------------------------
DROP TABLE IF EXISTS "_t_bot_account_context_6_old_20250215";
CREATE TABLE "_t_bot_account_context_6_old_20250215"(
            "id"                   INTEGER PRIMARY KEY AUTOINCREMENT,
            "bot_id"               INTEGER NOT NULL,
            "bot_key"              TEXT NOT NULL,
            "account_base_info_id" INTEGER,
            "twitter_id"           INtEGER,
            "discord_id"           INTEGER,
            "proxy_id"             INTEGER,
            "browser_env_id"       INTEGER,
            "telegram_id"          INTEGER,
            "wallet_id"            INTEGER,
            "reward_id"            INTEGER,
            "status"               INTEGER,
            "usable"               INTEGER(1),
            "params"               TEXT,
            "insert_datetime"      TEXT,
            "update_datetime"      TEXT,
            "is_valid"             INTEGER(1),
            UNIQUE (bot_key, account_base_info_id)
            );

-- ----------------------------
-- Table structure for _t_bot_account_context_8_old_20250216
-- ----------------------------
DROP TABLE IF EXISTS "_t_bot_account_context_8_old_20250216";
CREATE TABLE "_t_bot_account_context_8_old_20250216"(
            "id"                   INTEGER PRIMARY KEY AUTOINCREMENT,
            "bot_id"               INTEGER NOT NULL,
            "bot_key"              TEXT NOT NULL,
            "account_base_info_id" INTEGER,
            "twitter_id"           INtEGER,
            "discord_id"           INTEGER,
            "proxy_id"             INTEGER,
            "browser_env_id"       INTEGER,
            "telegram_id"          INTEGER,
            "wallet_id"            INTEGER,
            "reward_id"            INTEGER,
            "status"               INTEGER,
            "usable"               INTEGER(1),
            "params"               TEXT,
            "insert_datetime"      TEXT,
            "update_datetime"      TEXT,
            "is_valid"             INTEGER(1),
            UNIQUE (bot_key, account_base_info_id)
            );

-- ----------------------------
-- Table structure for _t_bot_info_old_20250216
-- ----------------------------
DROP TABLE IF EXISTS "_t_bot_info_old_20250216";
CREATE TABLE "_t_bot_info_old_20250216"(
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
name TEXT nuo null,
describe TEXT,
limit_project_ids TEXT,
  "insert_datetime" TEXT,
  "update_datetime" TEXT,
  "is_valid" INTEGER(1)
);

-- ----------------------------
-- Table structure for _t_bot_info_old_20250216_1
-- ----------------------------
DROP TABLE IF EXISTS "_t_bot_info_old_20250216_1";
CREATE TABLE "_t_bot_info_old_20250216_1" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "name" TEXT nuo NOT NULL,
  "describe" TEXT,
  "limit_project_ids" TEXT,
  "insert_datetime" TEXT,
  "update_datetime" TEXT,
  "is_valid" INTEGER(1)
);

-- ----------------------------
-- Table structure for _t_bot_info_old_20250216_2
-- ----------------------------
DROP TABLE IF EXISTS "_t_bot_info_old_20250216_2";
CREATE TABLE "_t_bot_info_old_20250216_2" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "name" TEXT nuo NOT NULL,
  "describe" TEXT,
  "limit_project_ids" TEXT,
  "params" TEXT,
  "insert_datetime" TEXT,
  "update_datetime" TEXT,
  "is_valid" INTEGER(1)
);

-- ----------------------------
-- Table structure for sqlite_sequence
-- ----------------------------
DROP TABLE IF EXISTS "sqlite_sequence";
CREATE TABLE sqlite_sequence(name,seq);

-- ----------------------------
-- Table structure for t_account_base_info
-- ----------------------------
DROP TABLE IF EXISTS "t_account_base_info";
CREATE TABLE "t_account_base_info" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "type" TEXT NOT NULL,
  "name" TEXT,
  "email" TEXT NOT NULL,
  "password" TEXT NOT NULL,
  "params" TEXT,
  "insert_datetime" TEXT,
  "update_datetime" TEXT,
  "is_valid" INTEGER(1)
);

-- ----------------------------
-- Table structure for t_bot_account_context
-- ----------------------------
DROP TABLE IF EXISTS "t_bot_account_context";
CREATE TABLE "t_bot_account_context" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "bot_id" INTEGER NOT NULL,
  "bot_key" TEXT NOT NULL,
  "account_base_info_id" INTEGER,
  "twitter_id" INtEGER,
  "discord_id" INTEGER,
  "proxy_id" INTEGER,
  "browser_env_id" INTEGER,
  "telegram_id" INTEGER,
  "wallet_id" INTEGER,
  "reward_id" INTEGER,
  "status" INTEGER,
  "params" TEXT,
  "insert_datetime" TEXT,
  "update_datetime" TEXT,
  "is_valid" INTEGER(1)
);

-- ----------------------------
-- Table structure for t_bot_account_context_10_3Mods_Google
-- ----------------------------
DROP TABLE IF EXISTS "t_bot_account_context_10_3Mods_Google";
CREATE TABLE t_bot_account_context_10_3Mods_Google
        (
            "id"
            INTEGER
            PRIMARY
            KEY
            AUTOINCREMENT,
            "bot_id"
            INTEGER
            NOT
            NULL,
            "bot_key"
            TEXT
            NOT
            NULL,
            "account_base_info_id"
            INTEGER,
            "twitter_id"
            INtEGER,
            "discord_id"
            INTEGER,
            "proxy_id"
            INTEGER,
            "browser_env_id"
            INTEGER,
            "telegram_id"
            INTEGER,
            "wallet_id"
            INTEGER,
            "reward_id"
            INTEGER,
            "status"
            INTEGER,
            "usable"
            INTEGER
        (
            1
        ),
            "params" TEXT,
            "insert_datetime" TEXT,
            "update_datetime" TEXT,
            "is_valid" INTEGER
        (
            1
        ),
            UNIQUE
        (
            bot_key,
            account_base_info_id,
            proxy_id
        )
            );

-- ----------------------------
-- Table structure for t_bot_account_context_10_3Mods_Xinglan
-- ----------------------------
DROP TABLE IF EXISTS "t_bot_account_context_10_3Mods_Xinglan";
CREATE TABLE t_bot_account_context_10_3Mods_Xinglan
        (
            "id"
            INTEGER
            PRIMARY
            KEY
            AUTOINCREMENT,
            "bot_id"
            INTEGER
            NOT
            NULL,
            "bot_key"
            TEXT
            NOT
            NULL,
            "account_base_info_id"
            INTEGER,
            "twitter_id"
            INtEGER,
            "discord_id"
            INTEGER,
            "proxy_id"
            INTEGER,
            "browser_env_id"
            INTEGER,
            "telegram_id"
            INTEGER,
            "wallet_id"
            INTEGER,
            "reward_id"
            INTEGER,
            "status"
            INTEGER,
            "usable"
            INTEGER
        (
            1
        ),
            "params" TEXT,
            "insert_datetime" TEXT,
            "update_datetime" TEXT,
            "is_valid" INTEGER
        (
            1
        ),
            UNIQUE
        (
            bot_key,
            account_base_info_id,
            proxy_id
        )
            );

-- ----------------------------
-- Table structure for t_bot_account_context_6
-- ----------------------------
DROP TABLE IF EXISTS "t_bot_account_context_6";
CREATE TABLE "t_bot_account_context_6" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "bot_id" INTEGER NOT NULL,
  "bot_key" TEXT NOT NULL,
  "account_base_info_id" INTEGER,
  "twitter_id" INtEGER,
  "discord_id" INTEGER,
  "proxy_id" INTEGER,
  "browser_env_id" INTEGER,
  "telegram_id" INTEGER,
  "wallet_id" INTEGER,
  "reward_id" INTEGER,
  "status" INTEGER,
  "usable" INTEGER(1),
  "params" TEXT,
  "insert_datetime" TEXT,
  "update_datetime" TEXT,
  "is_valid" INTEGER(1),
  UNIQUE ("bot_key" ASC, "account_base_info_id" ASC, "proxy_id" ASC)
);

-- ----------------------------
-- Table structure for t_bot_account_context_6_test
-- ----------------------------
DROP TABLE IF EXISTS "t_bot_account_context_6_test";
CREATE TABLE t_bot_account_context_6_test
        (
            "id"
            INTEGER
            PRIMARY
            KEY
            AUTOINCREMENT,
            "bot_id"
            INTEGER
            NOT
            NULL,
            "bot_key"
            TEXT
            NOT
            NULL,
            "account_base_info_id"
            INTEGER,
            "twitter_id"
            INtEGER,
            "discord_id"
            INTEGER,
            "proxy_id"
            INTEGER,
            "browser_env_id"
            INTEGER,
            "telegram_id"
            INTEGER,
            "wallet_id"
            INTEGER,
            "reward_id"
            INTEGER,
            "status"
            INTEGER,
            "usable"
            INTEGER
        (
            1
        ),
            "params" TEXT,
            "insert_datetime" TEXT,
            "update_datetime" TEXT,
            "is_valid" INTEGER
        (
            1
        ),
            UNIQUE
        (
            bot_key,
            account_base_info_id,
            proxy_id
        )
            );

-- ----------------------------
-- Table structure for t_bot_account_context_8
-- ----------------------------
DROP TABLE IF EXISTS "t_bot_account_context_8";
CREATE TABLE "t_bot_account_context_8" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "bot_id" INTEGER NOT NULL,
  "bot_key" TEXT NOT NULL,
  "account_base_info_id" INTEGER,
  "twitter_id" INtEGER,
  "discord_id" INTEGER,
  "proxy_id" INTEGER,
  "browser_env_id" INTEGER,
  "telegram_id" INTEGER,
  "wallet_id" INTEGER,
  "reward_id" INTEGER,
  "status" INTEGER,
  "usable" INTEGER(1),
  "params" TEXT,
  "insert_datetime" TEXT,
  "update_datetime" TEXT,
  "is_valid" INTEGER(1),
  UNIQUE ("bot_key" ASC, "account_base_info_id" ASC, "proxy_id" ASC)
);

-- ----------------------------
-- Table structure for t_bot_account_context_9
-- ----------------------------
DROP TABLE IF EXISTS "t_bot_account_context_9";
CREATE TABLE t_bot_account_context_9(
            "id"                   INTEGER PRIMARY KEY AUTOINCREMENT,
            "bot_id"               INTEGER NOT NULL,
            "bot_key"              TEXT NOT NULL,
            "account_base_info_id" INTEGER,
            "twitter_id"           INtEGER,
            "discord_id"           INTEGER,
            "proxy_id"             INTEGER,
            "browser_env_id"       INTEGER,
            "telegram_id"          INTEGER,
            "wallet_id"            INTEGER,
            "reward_id"            INTEGER,
            "status"               INTEGER,
            "usable"               INTEGER(1),
            "params"               TEXT,
            "insert_datetime"      TEXT,
            "update_datetime"      TEXT,
            "is_valid"             INTEGER(1),
            UNIQUE (bot_key, account_base_info_id, proxy_id)
            );

-- ----------------------------
-- Table structure for t_bot_info
-- ----------------------------
DROP TABLE IF EXISTS "t_bot_info";
CREATE TABLE "t_bot_info" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "name" TEXT nuo NOT NULL,
  "describe" TEXT,
  "limit_project_ids" TEXT,
  "image" TEXT,
  "job_params" TEXT,
  "params" TEXT,
  "insert_datetime" TEXT,
  "update_datetime" TEXT,
  "is_valid" INTEGER(1)
);

-- ----------------------------
-- Table structure for t_bot_instance
-- ----------------------------
DROP TABLE IF EXISTS "t_bot_instance";
CREATE TABLE "t_bot_instance" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
 "bot_id" INTEGER NOT NULL,
  "bot_key" text NOT NULL,
  "account_table_name" TEXT,
    "params" TEXT,
   "insert_datetime" TEXT,
   "update_datetime" TEXT,
    "is_valid" INTEGER(1),
    UNIQUE ("bot_id" ASC, "bot_key" ASC)
  );

-- ----------------------------
-- Table structure for t_browser_env
-- ----------------------------
DROP TABLE IF EXISTS "t_browser_env";
CREATE TABLE "t_browser_env" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "user_agent" TEXT NOT NULL,
  "other_header" text,
  "insert_datetime" TEXT,
  "update_datetime" TEXT,
  "is_valid" INTEGER(1)
);

-- ----------------------------
-- Table structure for t_discord_account
-- ----------------------------
DROP TABLE IF EXISTS "t_discord_account";
CREATE TABLE "t_discord_account" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "password" TEXT NOT NULL,
  "username" TEXT NOT NULL,
  "bind_email" TEXT,
  "bind_email_password" TEXT,
  "token" TEXT,
  "params" TEXT,
  "insert_datetime" TEXT,
  "update_datetime" TEXT,
  "is_valid" INTEGER(1)
);

-- ----------------------------
-- Table structure for t_project_info
-- ----------------------------
DROP TABLE IF EXISTS "t_project_info";
CREATE TABLE t_project_info(
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
name TEXT nuo null,
describe TEXT,

  "insert_datetime" TEXT,
  "update_datetime" TEXT,
  "is_valid" INTEGER(1)
);

-- ----------------------------
-- Table structure for t_proxy_info
-- ----------------------------
DROP TABLE IF EXISTS "t_proxy_info";
CREATE TABLE "t_proxy_info" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "proxy_type" TEXT,
  "proxy_protocol" TEXT,
  "host" TEXT NOT NULL,
  "port" INTEGER NOT NULL,
  "username" TEXT,
  "password" TEXT,
  "usable" INTEGER(1),
  "params" TEXT,
  "insert_datetime" TEXT,
  "update_datetime" TEXT,
  "is_valid" INTEGER(1)
);

-- ----------------------------
-- Table structure for t_reword_info
-- ----------------------------
DROP TABLE IF EXISTS "t_reword_info";
CREATE TABLE "t_reword_info" (
  "project_account_id" INTEGER,
  "total_points" REAL,
  "session" TEXT,
  "session_points" REAL,
  "daily_points" REAL,
  "insert_datetime" TEXT,
  "update_datetime" TEXT,
  "is_valid" INTEGER(1),
  PRIMARY KEY ("project_account_id")
);

-- ----------------------------
-- Table structure for t_telegram_account
-- ----------------------------
DROP TABLE IF EXISTS "t_telegram_account";
CREATE TABLE "t_telegram_account" (
  "id" INTEGER,
  "username" TEXT,
  "password" TEXT,
  "phone_prefix" TEXT NOT NULL,
  "phone" TEXT NOT NULL,
  "token" TEXT,
  "params" TEXT,
  "insert_datetime" TEXT,
  "update_datetime" TEXT,
  "is_valid" INTEGER(1),
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for t_twitter_account
-- ----------------------------
DROP TABLE IF EXISTS "t_twitter_account";
CREATE TABLE "t_twitter_account" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "username" TEXT NOT NULL,
  "password" TEXT NOT NULL,
  "email" TEXT,
  "email_password" TEXT,
  "token" TEXT,
  "f2a_key" TEXT,
  "params" TEXT,
  "insert_datetime" TEXT,
  "update_datetime" TEXT,
  "is_valid" INTEGER(1)
);

-- ----------------------------
-- Auto increment value for _t_bot_account_context_6_old_20250215
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 50 WHERE name = '_t_bot_account_context_6_old_20250215';

-- ----------------------------
-- Auto increment value for _t_bot_account_context_8_old_20250216
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 448 WHERE name = '_t_bot_account_context_8_old_20250216';

-- ----------------------------
-- Auto increment value for _t_bot_info_old_20250216
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 9 WHERE name = '_t_bot_info_old_20250216';

-- ----------------------------
-- Auto increment value for _t_bot_info_old_20250216_1
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 9 WHERE name = '_t_bot_info_old_20250216_1';

-- ----------------------------
-- Auto increment value for _t_bot_info_old_20250216_2
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 9 WHERE name = '_t_bot_info_old_20250216_2';

-- ----------------------------
-- Auto increment value for t_account_base_info
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 325 WHERE name = 't_account_base_info';

-- ----------------------------
-- Indexes structure for table t_account_base_info
-- ----------------------------
CREATE UNIQUE INDEX "main"."t_abi_e_idx"
ON "t_account_base_info" (
  "email" ASC
);

-- ----------------------------
-- Auto increment value for t_bot_account_context
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 403 WHERE name = 't_bot_account_context';

-- ----------------------------
-- Indexes structure for table t_bot_account_context
-- ----------------------------
CREATE UNIQUE INDEX "main"."t_bac_bik_idx"
ON "t_bot_account_context" (
  "bot_key" ASC,
  "account_base_info_id" ASC
);

-- ----------------------------
-- Auto increment value for t_bot_account_context_10_3Mods_Google
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 1805 WHERE name = 't_bot_account_context_10_3Mods_Google';

-- ----------------------------
-- Auto increment value for t_bot_account_context_10_3Mods_Xinglan
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 124 WHERE name = 't_bot_account_context_10_3Mods_Xinglan';

-- ----------------------------
-- Auto increment value for t_bot_account_context_6
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 102 WHERE name = 't_bot_account_context_6';

-- ----------------------------
-- Auto increment value for t_bot_account_context_8
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 2674 WHERE name = 't_bot_account_context_8';

-- ----------------------------
-- Auto increment value for t_bot_account_context_9
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 205 WHERE name = 't_bot_account_context_9';

-- ----------------------------
-- Auto increment value for t_bot_info
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 198 WHERE name = 't_bot_info';

-- ----------------------------
-- Indexes structure for table t_bot_info
-- ----------------------------
CREATE UNIQUE INDEX "main"."t_bi_n_idx"
ON "t_bot_info" (
  "name" ASC
);

-- ----------------------------
-- Auto increment value for t_bot_instance
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 85 WHERE name = 't_bot_instance';

-- ----------------------------
-- Auto increment value for t_browser_env
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 100 WHERE name = 't_browser_env';

-- ----------------------------
-- Indexes structure for table t_browser_env
-- ----------------------------
CREATE UNIQUE INDEX "main"."t_be_ua_idx"
ON "t_browser_env" (
  "user_agent" ASC
);

-- ----------------------------
-- Auto increment value for t_discord_account
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 7 WHERE name = 't_discord_account';

-- ----------------------------
-- Indexes structure for table t_discord_account
-- ----------------------------
CREATE UNIQUE INDEX "main"."t_da_u_idx"
ON "t_discord_account" (
  "username" ASC
);

-- ----------------------------
-- Auto increment value for t_proxy_info
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 200 WHERE name = 't_proxy_info';

-- ----------------------------
-- Indexes structure for table t_proxy_info
-- ----------------------------
CREATE UNIQUE INDEX "main"."t_pi_hp_idx"
ON "t_proxy_info" (
  "host" ASC,
  "port" ASC
);

-- ----------------------------
-- Indexes structure for table t_telegram_account
-- ----------------------------
CREATE UNIQUE INDEX "main"."t_ta_p_pn_idx"
ON "t_telegram_account" (
  "phone_prefix" ASC,
  "phone" ASC
);

-- ----------------------------
-- Auto increment value for t_twitter_account
-- ----------------------------
UPDATE "main"."sqlite_sequence" SET seq = 50 WHERE name = 't_twitter_account';

-- ----------------------------
-- Indexes structure for table t_twitter_account
-- ----------------------------
CREATE UNIQUE INDEX "main"."t_twitter_u_idx"
ON "t_twitter_account" (
  "username" ASC
);

PRAGMA foreign_keys = true;
