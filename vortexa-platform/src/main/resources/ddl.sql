create table t_account_base_info
(
    id              int auto_increment
        primary key,
    type            varchar(36)                          not null,
    name            varchar(36)                          null,
    email           varchar(64)                          not null,
    password        varchar(64)                          not null,
    params          text                                 null,
    insert_datetime datetime   default CURRENT_TIMESTAMP null,
    update_datetime datetime   default CURRENT_TIMESTAMP null,
    valid           tinyint(1) default 1                 null,
    constraint email
        unique (email)
);

create table t_bot_info
(
    id                int auto_increment
        primary key,
    name              varchar(36)                          not null,
    description       varchar(1024)                        null,
    limit_project_ids varchar(128)                         null,
    image             varchar(128)                         null,
    job_params        text                                 null,
    params            text                                 null,
    insert_datetime   datetime   default CURRENT_TIMESTAMP null,
    update_datetime   datetime   default CURRENT_TIMESTAMP null,
    valid             tinyint(1) default 1                 null,
    version           varchar(64)                          null,
    constraint name
        unique (name)
);

create table t_bot_instance
(
    id                 int auto_increment
        primary key,
    bot_id             int                                  not null,
    bot_name           varchar(36)                          null,
    script_node_name   varchar(255)                         null,
    bot_key            varchar(36)                          not null,
    account_table_name varchar(128)                         null,
    job_params         text                                 null,
    params             text                                 null,
    version            varchar(64)                          null,
    insert_datetime    datetime   default CURRENT_TIMESTAMP null,
    update_datetime    datetime                             null,
    valid              tinyint(1) default 1                 null,
    constraint bot_id
        unique (bot_id, script_node_name, bot_key)
);

create table t_browser_env
(
    id              int auto_increment
        primary key,
    user_agent      varchar(555)                         not null,
    other_header    text                                 null,
    insert_datetime datetime   default CURRENT_TIMESTAMP null,
    update_datetime datetime                             null,
    valid           tinyint(1) default 1                 null,
    constraint user_agent
        unique (user_agent)
);

create table t_discord_account
(
    id                  int auto_increment
        primary key,
    password            varchar(64)                          not null,
    username            varchar(64)                          not null,
    bind_email          varchar(64)                          null,
    bind_email_password varchar(64)                          null,
    token               varchar(256)                         null,
    params              text                                 null,
    insert_datetime     datetime   default CURRENT_TIMESTAMP null,
    update_datetime     datetime                             null,
    valid               tinyint(1) default 1                 null,
    constraint username
        unique (username)
);

create table t_project_info
(
    id              int auto_increment
        primary key,
    name            varchar(64)                          null,
    description     varchar(5000)                        null,
    insert_datetime datetime   default CURRENT_TIMESTAMP null,
    update_datetime datetime                             null,
    valid           tinyint(1) default 1                 null
);

create table t_proxy_info
(
    id              int auto_increment
        primary key,
    proxy_type      varchar(32)                          null,
    proxy_protocol  varchar(32)                          null,
    host            varchar(64)                          not null,
    port            int                                  not null,
    username        varchar(64)                          null,
    password        varchar(64)                          null,
    usable          int                                  null,
    params          text                                 null,
    insert_datetime datetime   default CURRENT_TIMESTAMP null,
    update_datetime datetime                             null,
    valid           tinyint(1) default 1                 null,
    constraint host
        unique (host, port)
);

create table t_reword_info
(
    project_account_id int                                  not null
        primary key,
    total_points       double                               null,
    session            varchar(64)                          null,
    session_points     double                               null,
    daily_points       double                               null,
    insert_datetime    datetime   default CURRENT_TIMESTAMP null,
    update_datetime    datetime                             null,
    valid              tinyint(1) default 1                 null
);

create table t_reword_info_994_test_bot_main
(
    id              int auto_increment
        primary key,
    bot_id          int                                  not null,
    bot_key         varchar(255)                         not null,
    bot_account_id  int                                  not null,
    total_points    double                               null,
    daily_points    double                               null,
    insert_datetime datetime   default CURRENT_TIMESTAMP null,
    update_datetime datetime                             null,
    valid           tinyint(1) default 1                 null
);

create table t_script_node
(
    id                bigint auto_increment
        primary key,
    group_id          varchar(255)                         null,
    service_id        varchar(255)                         null,
    instance_id       varchar(255)                         null,
    host              varchar(255)                         null,
    port              int                                  null,
    script_node_name  varchar(255)                         null,
    description       text                                 null,
    node_app_config   text                                 null,
    bot_meta_info_map text                                 null,
    bot_config_map    text                                 null,
    params            text                                 null,
    version           varchar(64)                          null,
    insert_datetime   datetime   default CURRENT_TIMESTAMP null,
    update_datetime   datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    valid             tinyint(1) default 1                 null,
    constraint `group`
        unique (group_id, service_id, instance_id),
    constraint t_script_node_script_node_name_uindex
        unique (script_node_name)
);

create table t_telegram_account
(
    id              int auto_increment
        primary key,
    username        varchar(64)                          null,
    password        varchar(64)                          null,
    phone_prefix    varchar(8)                           not null,
    phone           varchar(32)                          not null,
    token           varchar(256)                         null,
    params          text                                 null,
    insert_datetime datetime   default CURRENT_TIMESTAMP null,
    update_datetime datetime                             null,
    valid           tinyint(1) default 1                 null,
    constraint phone_prefix
        unique (phone_prefix, phone)
);

create table t_twitter_account
(
    id              int auto_increment
        primary key,
    username        varchar(64)                          not null,
    password        varchar(64)                          not null,
    email           varchar(64)                          null,
    email_password  varchar(64)                          null,
    token           varchar(256)                         null,
    f2a_key         varchar(256)                         null,
    params          text                                 null,
    insert_datetime datetime   default CURRENT_TIMESTAMP null,
    update_datetime datetime                             null,
    valid           tinyint(1) default 1                 null,
    constraint username
        unique (username)
);

create table t_web3_wallet
(
    id              bigint auto_increment
        primary key,
    mnemonic        varchar(520)                         null,
    eth_private_key varchar(520)                         null,
    eth_address     varchar(520)                         null,
    sol_address     varchar(520)                         null,
    sol_private_key varchar(520)                         null,
    btc_private_key varchar(520)                         null,
    btc_address     varchar(520)                         null,
    params          text                                 null,
    insert_datetime datetime   default CURRENT_TIMESTAMP null,
    update_datetime datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    valid           tinyint(1) default 1                 null,
    constraint mnemonic
        unique (mnemonic)
);

