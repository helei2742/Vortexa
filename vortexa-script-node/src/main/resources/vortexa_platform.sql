/*
 Navicat Premium Data Transfer

 Source Server         : macLocal
 Source Server Type    : MySQL
 Source Server Version : 80029 (8.0.29)
 Source Host           : localhost:3306
 Source Schema         : vortexa_platform

 Target Server Type    : MySQL
 Target Server Version : 80029 (8.0.29)
 File Encoding         : 65001

 Date: 08/04/2025 00:54:52
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_account_base_info
-- ----------------------------
DROP TABLE IF EXISTS `t_account_base_info`;
CREATE TABLE `t_account_base_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `type` varchar(36) NOT NULL,
  `name` varchar(36) DEFAULT NULL,
  `email` varchar(64) NOT NULL,
  `password` varchar(64) NOT NULL,
  `params` text,
  `insert_datetime` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_datetime` datetime DEFAULT CURRENT_TIMESTAMP,
  `valid` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=353 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for t_bot_info
-- ----------------------------
DROP TABLE IF EXISTS `t_bot_info`;
CREATE TABLE `t_bot_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(36) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `limit_project_ids` varchar(128) DEFAULT NULL,
  `image` varchar(128) DEFAULT NULL,
  `job_params` text,
  `params` text,
  `insert_datetime` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_datetime` datetime DEFAULT CURRENT_TIMESTAMP,
  `valid` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for t_bot_instance
-- ----------------------------
DROP TABLE IF EXISTS `t_bot_instance`;
CREATE TABLE `t_bot_instance` (
  `id` int NOT NULL AUTO_INCREMENT,
  `bot_id` int NOT NULL,
  `bot_name` varchar(36) DEFAULT NULL,
  `bot_key` varchar(36) NOT NULL,
  `account_table_name` varchar(128) DEFAULT NULL,
  `job_params` text,
  `params` text,
  `insert_datetime` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_datetime` datetime DEFAULT NULL,
  `valid` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `bot_id` (`bot_id`,`bot_key`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for t_browser_env
-- ----------------------------
DROP TABLE IF EXISTS `t_browser_env`;
CREATE TABLE `t_browser_env` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_agent` varchar(555) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `other_header` text,
  `insert_datetime` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_datetime` datetime DEFAULT NULL,
  `valid` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_agent` (`user_agent`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for t_discord_account
-- ----------------------------
DROP TABLE IF EXISTS `t_discord_account`;
CREATE TABLE `t_discord_account` (
  `id` int NOT NULL AUTO_INCREMENT,
  `password` varchar(64) NOT NULL,
  `username` varchar(64) NOT NULL,
  `bind_email` varchar(64) DEFAULT NULL,
  `bind_email_password` varchar(64) DEFAULT NULL,
  `token` varchar(256) DEFAULT NULL,
  `params` text,
  `insert_datetime` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_datetime` datetime DEFAULT NULL,
  `valid` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for t_project_info
-- ----------------------------
DROP TABLE IF EXISTS `t_project_info`;
CREATE TABLE `t_project_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(64) DEFAULT NULL,
  `description` varchar(5000) DEFAULT NULL,
  `insert_datetime` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_datetime` datetime DEFAULT NULL,
  `valid` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for t_proxy_info
-- ----------------------------
DROP TABLE IF EXISTS `t_proxy_info`;
CREATE TABLE `t_proxy_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `proxy_type` varchar(32) DEFAULT NULL,
  `proxy_protocol` varchar(32) DEFAULT NULL,
  `host` varchar(64) NOT NULL,
  `port` int NOT NULL,
  `username` varchar(64) DEFAULT NULL,
  `password` varchar(64) DEFAULT NULL,
  `usable` int DEFAULT NULL,
  `params` text,
  `insert_datetime` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_datetime` datetime DEFAULT NULL,
  `valid` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `host` (`host`,`port`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for t_reword_info
-- ----------------------------
DROP TABLE IF EXISTS `t_reword_info`;
CREATE TABLE `t_reword_info` (
  `project_account_id` int NOT NULL,
  `total_points` double DEFAULT NULL,
  `session` varchar(64) DEFAULT NULL,
  `session_points` double DEFAULT NULL,
  `daily_points` double DEFAULT NULL,
  `insert_datetime` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_datetime` datetime DEFAULT NULL,
  `valid` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`project_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for t_telegram_account
-- ----------------------------
DROP TABLE IF EXISTS `t_telegram_account`;
CREATE TABLE `t_telegram_account` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(64) DEFAULT NULL,
  `password` varchar(64) DEFAULT NULL,
  `phone_prefix` varchar(8) NOT NULL,
  `phone` varchar(32) NOT NULL,
  `token` varchar(256) DEFAULT NULL,
  `params` text,
  `insert_datetime` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_datetime` datetime DEFAULT NULL,
  `valid` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `phone_prefix` (`phone_prefix`,`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for t_twitter_account
-- ----------------------------
DROP TABLE IF EXISTS `t_twitter_account`;
CREATE TABLE `t_twitter_account` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password` varchar(64) NOT NULL,
  `email` varchar(64) DEFAULT NULL,
  `email_password` varchar(64) DEFAULT NULL,
  `token` varchar(256) DEFAULT NULL,
  `f2a_key` varchar(256) DEFAULT NULL,
  `params` text,
  `insert_datetime` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_datetime` datetime DEFAULT NULL,
  `valid` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=251 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;

create table t_script_node
(
    id              bigint auto_increment
        primary key,
    group_id        varchar(255)                         null,
    service_id      varchar(255)                         null,
    instance_id     varchar(255)                         null,
    host            varchar(255)                         null,
    port            int                                  null,
    script_node_name       varchar(255)                         null,
    description     text                                 null,
    bot_config_map  text                                 null,
    params          text                                 null,
    insert_datetime datetime   default CURRENT_TIMESTAMP null,
    update_datetime datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    valid           tinyint(1) default 1                 null,
    constraint `group`
        unique (group_id, service_id, instance_id)
);
