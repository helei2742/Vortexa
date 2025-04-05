package cn.com.vortexa.script_node.view;

import cn.com.vortexa.script_node.bot.JobInvokeAutoBot;
import cn.com.vortexa.script_node.util.AccountInfoPrinter;
import cn.com.vortexa.script_node.view.commandMenu.CommandMenuNode;
import cn.com.vortexa.script_node.view.commandMenu.DefaultMenuType;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.script_node.view.commandMenu.PageMenuNode;
import cn.com.vortexa.common.dto.ACListOptResult;
import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.common.entity.BrowserEnv;
import cn.com.vortexa.common.entity.ProxyInfo;
import cn.com.vortexa.common.entity.RewordInfo;
import cn.com.vortexa.job.constants.JobStatus;
import com.alibaba.fastjson.JSON;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static cn.com.vortexa.script_node.constants.MapConfigKey.*;


@Slf4j
public class ScriptNodeCMDLineMenu extends CommandLineMenu {
    /**
     * 刷新节点
     */
    public static final CommandMenuNode REFRESH_NODE = new CommandMenuNode(true, "刷新", "当前数据已刷新", null);

    private final List<DefaultMenuType> defaultMenuTypes;

    @Setter
    private Consumer<CommandMenuNode> addCustomMenuNode;

    public ScriptNodeCMDLineMenu(List<DefaultMenuType> defaultMenuTypes) {
        super();
        this.defaultMenuTypes = new ArrayList<>(defaultMenuTypes);

        this.defaultMenuTypes.add(DefaultMenuType.IMPORT);
        this.defaultMenuTypes.add(DefaultMenuType.ACCOUNT_LIST);
        this.defaultMenuTypes.add(DefaultMenuType.PROXY_LIST);
        this.defaultMenuTypes.add(DefaultMenuType.BROWSER_ENV_LIST);
    }


    @Override
    public final void buildBotMenuNode(CommandMenuNode botMenuNode, JobInvokeAutoBot bot) {
        if (addCustomMenuNode != null) {
            addCustomMenuNode.accept(botMenuNode);
        }

        for (DefaultMenuType menuType : defaultMenuTypes) {
            botMenuNode.addSubMenu(switch (menuType) {
                case REGISTER -> buildRegisterMenuNode();
                case VERIFIER -> buildVerifierMenuNode();
                case LOGIN -> buildQueryTokenMenuNode();
                case ACCOUNT_LIST -> buildAccountListMenuNode();
                case PROXY_LIST -> buildProxyListMenuNode();
                case BROWSER_ENV_LIST -> buildBrowserListMenuNode();
                case START_BOT_TASK -> buildStartBotTaskMenuNode(bot);
                case IMPORT -> buildImportMenuNode();
            });
        }
    }


    /**
     * 构建注册菜单节点
     *
     * @return CommandMenuNode
     */
    private CommandMenuNode buildRegisterMenuNode() {
        CommandMenuNode registerMenu = new CommandMenuNode("注册",
                "请确认设置后运行", this::printCurrentRegisterConfig);

        CommandMenuNode interInvite = new CommandMenuNode(
                "填入邀请码",
                "请输入邀请码：",
                this::printCurrentRegisterConfig
        );
        interInvite.setResolveInput(input -> {
            AutoBotConfig autoBotConfig = getBot().getAutoBotConfig();
            log.info("邀请码修改[{}]->[{}]", autoBotConfig.getConfig(INVITE_CODE_KEY), input);
            autoBotConfig.setConfig(INVITE_CODE_KEY, input);
        });

        return registerMenu
                .addSubMenu(interInvite)
                .addSubMenu(new CommandMenuNode(
                        true,
                        "开始注册",
                        "开始注册所有账号...",
                        () -> {
                            try {

                                return getBot().registerAccount().get().toString();
                            } catch (Exception e) {
                                return "registry error, " + e.getMessage();
                            }
                        }
                ));
    }

    private CommandMenuNode buildVerifierMenuNode() {

        return new CommandMenuNode("验证邮箱", "请选择验证的账户类型",
                () -> "当前的邮箱类型：" + getBot().getAutoBotConfig().getConfig(EMAIL_VERIFIER_TYPE));
    }


    /**
     * 获取token
     *
     * @return CommandMenuNode
     */
    private CommandMenuNode buildQueryTokenMenuNode() {
        return new CommandMenuNode("获取token", "开始获取token", () -> {
            CompletableFuture<ACListOptResult> getToken = getBot().loginAndTakeTokenAccount();
            try {
                ACListOptResult acListOptResult = getToken.get();

                return acListOptResult.printStr();
            } catch (InterruptedException | ExecutionException e) {
                getBot().logger.error("获取token异常, " +
                        (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
                return "";
            }
        });
    }

    /**
     * 构建查看代理列表的菜单节点
     *
     * @return 查看代理列表菜单节点
     */
    private CommandMenuNode buildProxyListMenuNode() {
        return new PageMenuNode<>("查看代理列表", "当前代理列表:", (pageNum, pageSize) -> {
            try {
                return getBot().getBotApi().getProxyInfoRPC().conditionPageQueryRPC(pageNum, pageSize, new HashMap<>());
            } catch (SQLException e) {
                getBot().logger.error("查询代理列表出错, " + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
                return null;
            }
        }, ProxyInfo.class);
    }


    /**
     * 构建查看浏览器环境列表的菜单节点
     *
     * @return 查看浏览器环境列表菜单节点
     */
    private CommandMenuNode buildBrowserListMenuNode() {
        return new PageMenuNode<>("查看浏览器环境列表", "当前浏览器环境:", (pageNum, pageSize) -> {
            try {
                return getBot().getBotApi().getBrowserEnvRPC().conditionPageQueryRPC(pageNum, pageSize, new HashMap<>());
            } catch (SQLException e) {
                getBot().logger.error("查询浏览器环境列表出错, " + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
                return null;
            }
        }, BrowserEnv.class);
    }

    /**
     * 账户列表菜单节点
     *
     * @return 账户列表节点
     */
    private CommandMenuNode buildAccountListMenuNode() {
        CommandMenuNode accountListMenuNode = new PageMenuNode<>("查看账号", "当前账户详情列表:", (pageNum, pageSize) -> {
            try {
                HashMap<String, Object> filter = new HashMap<>();
                filter.put("botId", getBot().getBotInstance().getBotId());
                filter.put("botKey", getBot().getAutoBotConfig().getBotKey());

                PageResult<AccountContext> pageResult = getBot().getBotApi().getBotAccountService().conditionPageQuery(pageNum, pageSize, filter);
                getBot().getPersistenceManager().fillAccountInfos(pageResult.getList());
                return pageResult;
            } catch (Exception e) {
                getBot().logger.error("查询账号列表出错, " + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
                return null;
            }
        }, AccountContext.class);

        return accountListMenuNode
                .addSubMenu(buildAccountRewardMenuNode())
                .addSubMenu(buildAccountConnectStatusMenuNode())
                .addSubMenu(REFRESH_NODE);
    }

    /**
     * 查看账户收益菜单节点
     *
     * @return 账户收益节点
     */
    private CommandMenuNode buildAccountRewardMenuNode() {
        return new PageMenuNode<>("查看账号收益", "账号收益详情列表:", (pageNum, pageSize) -> {
            try {
                return getBot().getBotApi().getRewordInfoService().conditionPageQuery(pageNum, pageSize, new HashMap<>());
            } catch (SQLException e) {
                getBot().logger.error("查询账号收益列表出错, " + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
                return null;
            }
        }, RewordInfo.class);
    }

    /**
     * 查看账户连接情况菜单节点
     *
     * @return 账户收益节点
     */
    private CommandMenuNode buildAccountConnectStatusMenuNode() {
        return new CommandMenuNode(
                "查看账号连接情况",
                "账号连接情况列表:",
                () -> AccountInfoPrinter.printAccountConnectStatusList(getBot().getAccountContexts())
        ).addSubMenu(REFRESH_NODE);
    }


    /**
     * 开始账户连接菜单节点
     *
     * @return 连接账户菜单节点
     */
    private CommandMenuNode buildStartBotTaskMenuNode(JobInvokeAutoBot bot) {
        CommandMenuNode menuNode = new CommandMenuNode(
                "启动任务",
                "选择任务类型",
                null
        );

        Set<String> jobNameSet = bot.botJobNameList();
        for (String jobName : jobNameSet) {

            CommandMenuNode typeInput = new CommandMenuNode(true, null, "type",
                    () -> JSON.toJSONString(bot.startBotJob(jobName))
            );

            typeInput.setTittleBuilder(() -> {
                JobStatus status = null;
                try {
                    status = getBot().getBotApi().getBotJobService().queryJobStatus(getBot().getAutoBotConfig().getBotKey(), jobName);
                    return "%s 任务 (%s)".formatted(jobName, status);
                } catch (SchedulerException e) {
                    throw new RuntimeException(e);
                }
            });

            menuNode.addSubMenu(typeInput);
        }

        return menuNode;
    }

    /**
     * 导入菜单节点
     *
     * @return CommandMenuNode
     */
    private CommandMenuNode buildImportMenuNode() {

        return new CommandMenuNode("导入", "请选择要导入的数据", null)
                .addSubMenu(buildImportBotAccountContextMenuNode())
                // .addSubMenu(buildImportBaseAccountMenuNode())
                // .addSubMenu(buildImportProxyMenuNode())
                // .addSubMenu(buildImportBrowserEnvMenuNode())
                // .addSubMenu(buildImportTwitterMenuNode())
                // .addSubMenu(buildImportDiscordMenuNode())
                // .addSubMenu(buildImportTelegramMenuNode())
                ;
    }

    // /**
    //  * 导入浏览器环境菜单节点
    //  *
    //  * @return CommandMenuNode
    //  */
    // private CommandMenuNode buildImportBrowserEnvMenuNode() {
    //
    //     return new CommandMenuNode(true, "导入浏览器环境", null, () -> {
    //         String filePath = getBotConfig().getFilePathConfig().getBrowserEnvFileBotConfigPath();
    //
    //         try {
    //             getBot().getBotApi().getBrowserEnvRPC().importFromExcel(filePath);
    //         } catch (SQLException e) {
    //             throw new RuntimeException(e);
    //         }
    //
    //         return "浏览器环境导入完成";
    //     });
    // }
    //
    // /**
    //  * 导入代理信息
    //  *
    //  * @return CommandMenuNode
    //  */
    // private CommandMenuNode buildImportProxyMenuNode() {
    //     return new CommandMenuNode(true, "导入代理", null, () -> {
    //
    //         try {
    //             getBot().getBotApi().getProxyInfoRPC()
    //                     .importFromExcel(getBotConfig().getFilePathConfig().getProxyFileBotConfigPath());
    //         } catch (SQLException e) {
    //             throw new RuntimeException(e);
    //         }
    //
    //         return "代理导入完成";
    //     });
    // }
    //
    // /**
    //  * 导入账号基本信息
    //  *
    //  * @return CommandMenuNode
    //  */
    // private CommandMenuNode buildImportBaseAccountMenuNode() {
    //     return new CommandMenuNode(true, "导入账号基本信息", null, () -> {
    //
    //         try {
    //             Integer integer = getBot().getBotApi().getAccountBaseInfoRPC()
    //                     .importFromExcel(getBotConfig().getFilePathConfig().getBaseAccountFileBotConfigPath());
    //             return "账号基本信息导入完成，" + integer;
    //         } catch (SQLException e) {
    //             throw new RuntimeException(e);
    //         }
    //     });
    // }
    //
    //
    // /**
    //  * 导入twitter账号基本信息
    //  *
    //  * @return CommandMenuNode
    //  */
    // private CommandMenuNode buildImportTwitterMenuNode() {
    //     return new CommandMenuNode(true, "导入twitter账号", null, () -> {
    //         try {
    //             Integer i = getBot().getBotApi().getTwitterAccountRPC()
    //                     .importFromExcel(getBotConfig().getFilePathConfig().getTwitterFileBotConfigPath());
    //             return "twitter导入完成, " + i;
    //         } catch (SQLException e) {
    //             throw new RuntimeException(e);
    //         }
    //     });
    // }
    //
    //
    // /**
    //  * 导入discord账号基本信息
    //  *
    //  * @return CommandMenuNode
    //  */
    // private CommandMenuNode buildImportDiscordMenuNode() {
    //     return new CommandMenuNode(true, "导入discord账号", null, () -> {
    //
    //         try {
    //             Integer i = getBot().getBotApi().getDiscordAccountRPC().importFromExcel(getBotConfig().getFilePathConfig().getDiscordFileBotConfigPath());
    //             return "discord导入完成," + i;
    //         } catch (SQLException e) {
    //             throw new RuntimeException(e);
    //         }
    //     });
    // }
    //
    // /**
    //  * 导入Telegram账号基本信息
    //  *
    //  * @return CommandMenuNode
    //  */
    // private CommandMenuNode buildImportTelegramMenuNode() {
    //     return new CommandMenuNode(true, "导入Telegram账号", null, () -> {
    //
    //         try {
    //             Integer i = getBot().getBotApi().getTelegramAccountRPC().importFromExcel(getBotConfig().getFilePathConfig().getTelegramFileBotConfigPath());
    //
    //             return "Telegram导入完成" + i;
    //         } catch (SQLException e) {
    //             throw new RuntimeException(e);
    //         }
    //     });
    // }


    /**
     * 导入bot使用的账号菜单节点
     *
     * @return CommandMenuNode
     */
    private CommandMenuNode buildImportBotAccountContextMenuNode() {
        return new CommandMenuNode(true, "导入bot运行账号", null, () -> {

            try {
                Integer i = getBot().getBotApi().getBotAccountService().importFromExcel(
                        getBot().getBotInfo().getId(),
                        getBot().getBotInstance().getBotKey(),
                        getBot().getAutoBotConfig().getAccountConfig().getConfigFilePath()
                );
                getBot().initAccounts();
                return "bot运行账号导入完成," + i;
            } catch (Exception e) {
                return "import bot account context error," + e.getMessage();
            }
        });
    }


    /**
     * 打印当前的邀请码
     *
     * @return 邀请码
     */
    private String printCurrentRegisterConfig() {
        String inviteCode = (String) getBot().getAutoBotConfig().getCustomConfig().get(INVITE_CODE_KEY);
        String registerType = (String) getBot().getAutoBotConfig().getCustomConfig().get(REGISTER_TYPE_KEY);

        return "(当前邀请码为:" + inviteCode + ")\n"
                + "(当前注册类型为:" + registerType + ")\n";
    }
}
