create table t_account_base_info
(
    id              INT primary key auto_increment,
    type            VARCHAR(36) not null,
    name            VARCHAR(36),
    email           VARCHAR(64) not null,
    password        VARCHAR(64) not null,
    params          TEXT,
    insert_datetime DATETIME,
    update_datetime DATETIME,
    is_valid        INT(1),
    unique (email)
);

create table t_bot_info
(
    id                INT primary key auto_increment,
    name              VARCHAR(36) not null,
    description       VARCHAR(1024),
    limit_project_ids VARCHAR(128),
    image             VARCHAR(128),
    job_params        TEXT,
    params            TEXT,
    insert_datetime   DATETIME,
    update_datetime   DATETIME,
    is_valid          INT(1),
    unique (name)
);

create table t_bot_instance
(
    id                 INT primary key auto_increment,
    bot_id             INT         not null,
    bot_name           VARCHAR(36),
    bot_key            VARCHAR(36) not null,
    account_table_name VARCHAR(128),
    job_params         TEXT,
    params             TEXT,
    insert_datetime    DATETIME,
    update_datetime    DATETIME,
    is_valid           INT(1),
    unique (bot_id, bot_key)
);

create table t_browser_env
(
    id              INT
        primary key auto_increment,
    user_agent      VARCHAR(64) not null,
    other_header    text,
    insert_datetime DATETIME,
    update_datetime DATETIME,
    is_valid        INT(1),
    unique (user_agent)
);

create table t_discord_account
(
    id                  INT
        primary key auto_increment,
    password            VARCHAR(64) not null,
    username            VARCHAR(64) not null,
    bind_email          VARCHAR(64),
    bind_email_password VARCHAR(64),
    token               VARCHAR(256),
    params              TEXT,
    insert_datetime     DATETIME,
    update_datetime     DATETIME,
    is_valid            INT(1),
    unique (username)
);

create table t_project_info
(
    id              INT
        primary key auto_increment,
    name            VARCHAR(64),
    description     VARCHAR(5000),
    insert_datetime DATETIME,
    update_datetime DATETIME,
    is_valid        INT(1)
);

create table t_proxy_info
(
    id              INT
        primary key auto_increment,
    proxy_type      VARCHAR(32),
    proxy_protocol  VARCHAR(32),
    host            VARCHAR(64) not null,
    port            INT         not null,
    username        VARCHAR(64),
    password        VARCHAR(64),
    usable          INT(1),
    params          TEXT,
    insert_datetime DATETIME,
    update_datetime DATETIME,
    is_valid        INT(1),
    unique (host, port)
);

create table t_reword_info
(
    project_account_id INT
        primary key,
    total_points       double,
    session            VARCHAR(64),
    session_points     double,
    daily_points       double,
    insert_datetime    DATETIME,
    update_datetime    DATETIME,
    is_valid           INT(1)
);

create table t_telegram_account
(
    id              INT
        primary key,
    username        VARCHAR(64),
    password        VARCHAR(64),
    phone_prefix    VARCHAR(8)  not null,
    phone           VARCHAR(32) not null,
    token           VARCHAR(256),
    params          TEXT,
    insert_datetime DATETIME,
    update_datetime DATETIME,
    is_valid        INT(1),
    unique (phone_prefix, phone)
);


create table t_twitter_account
(
    id              INT
        primary key auto_increment,
    username        VARCHAR(64) not null,
    password        VARCHAR(64) not null,
    email           VARCHAR(64),
    email_password  VARCHAR(64),
    token           VARCHAR(256),
    f2a_key         VARCHAR(256),
    params          TEXT,
    insert_datetime DATETIME,
    update_datetime DATETIME,
    is_valid        INT(1),
    unique (username)
);

create table t_script_node
(
    id              bigint auto_increment
        primary key,
    group_id        varchar(255)                         null,
    service_id      varchar(255)                         null,
    instance_id     varchar(255)                         null,
    host            varchar(255)                         null,
    port            int                                  null,
    bot_group       varchar(255)                         null,
    description     text                                 null,
    bot_config_map  text                                 null,
    params          text                                 null,
    insert_datetime datetime   default CURRENT_TIMESTAMP null,
    update_datetime datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    valid           tinyint(1) default 1                 null,
    constraint `group`
        unique (group_id, service_id, instance_id)
);

