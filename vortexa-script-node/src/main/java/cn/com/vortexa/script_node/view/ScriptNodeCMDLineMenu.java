package cn.com.vortexa.script_node.view;

import cn.com.vortexa.common.util.AnsiColor;
import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import cn.com.vortexa.script_node.constants.BotStatus;
import cn.com.vortexa.script_node.util.AccountInfoPrinter;
import cn.com.vortexa.script_node.util.ScriptBotLauncher;
import cn.com.vortexa.script_node.view.commandMenu.CommandMenuNode;
import cn.com.vortexa.script_node.view.commandMenu.DefaultMenuType;
import cn.com.vortexa.script_node.view.commandMenu.PageMenuNode;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static cn.com.vortexa.script_node.constants.MapConfigKey.*;

@Slf4j
public class ScriptNodeCMDLineMenu extends CommandLineMenu {
    /**
     * 刷新节点
     */
    public static final CommandMenuNode REFRESH_NODE = new CommandMenuNode(true, "刷新", "当前数据已刷新");

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
    public final void buildBotMenuNode(CommandMenuNode botMenuNode, String botKey) {
        if (addCustomMenuNode != null) {
            addCustomMenuNode.accept(botMenuNode);
        }

        for (DefaultMenuType menuType : defaultMenuTypes) {
            botMenuNode.addSubMenu(switch (menuType) {
                case ACCOUNT_LIST -> buildAccountListMenuNode();
                case PROXY_LIST -> buildProxyListMenuNode();
                case BROWSER_ENV_LIST -> buildBrowserListMenuNode();
                case START_BOT_TASK -> buildStartBotTaskMenuNode(botKey);
                case IMPORT -> buildImportMenuNode();
                case LAUNCH_SCRIPT -> buildLaunchScriptMenuNode(botKey);
            });
        }
    }

    /**
     * 启动bot菜单
     *
     * @return CommandMenuNode
     */
    private CommandMenuNode buildLaunchScriptMenuNode(String botKey) {
        ScriptBotLauncher botLauncher = ScriptBotLauncher.INSTANCE;

        CommandMenuNode commandMenuNode = new CommandMenuNode("启动/关闭 Bot", "启动或关闭Bot",
                () -> "当前选择的Bot[%s]\n状态[%s]".formatted(
                        botKey,
                        botLauncher.getBotStatus(botKey)
                ));

        return commandMenuNode.addSubMenu(new CommandMenuNode(true, "启动", null, () -> {
            try {
                botLauncher.loadAndLaunchBot(botKey);
                return botKey + " launch finish...Current status: " + botLauncher.getBotStatus(botKey);
            } catch (Exception e) {
                log.error("start bot[{}] error", botKey, e);
                return "";
            }
        })).addSubMenu(new CommandMenuNode(true, "关闭", null, () -> {
            AutoLaunchBot<?> keyBot = botLauncher.getBotByBotKey(botKey);
            if (keyBot == null || keyBot.getStatus() != BotStatus.RUNNING) {
                return AnsiColor.colorize("bot当前状态不能关闭", AnsiColor.YELLOW);
            }
            keyBot.stop();
            return "";
        }));
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
                getBot().logger.error(
                        "查询代理列表出错, " + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
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
                return getBot().getBotApi()
                        .getBrowserEnvRPC()
                        .conditionPageQueryRPC(pageNum, pageSize, new HashMap<>());
            } catch (SQLException e) {
                getBot().logger.error(
                        "查询浏览器环境列表出错, " + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
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
        CommandMenuNode accountListMenuNode = new PageMenuNode<>("查看账号", "当前账户详情列表:",
                (pageNum, pageSize) -> {
                    try {
                        HashMap<String, Object> filter = new HashMap<>();
                        filter.put("botId", getBot().getBotInstance().getBotId());
                        filter.put("botKey", getBot().getAutoBotConfig().getBotKey());

                        PageResult<AccountContext> pageResult = getBot().getBotApi()
                                .getBotAccountService()
                                .conditionPageQuery(pageNum, pageSize, filter);
                        getBot().getPersistenceManager().fillAccountInfos(pageResult.getList());
                        return pageResult;
                    } catch (Exception e) {
                        getBot().logger.error(
                                "查询账号列表出错, " + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
                        return new PageResult<>();
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
                return getBot().getBotApi()
                        .getRewordInfoService()
                        .queryAccountReword(pageNum, pageSize, new HashMap<>());
            } catch (Exception e) {
                getBot().logger.error(
                        "查询账号收益列表出错, " + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
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
    private CommandMenuNode buildStartBotTaskMenuNode(String botKey) {
        return new CommandMenuNode(
                "启动任务",
                "选择任务类型",
                node -> {
                    AutoLaunchBot<?> bot = getLoadedBotMap().get(botKey);
                    if (bot == null) return null;

                    Set<String> existJobs = node.getSubNodeList()
                            .stream()
                            .map(CommandMenuNode::getDescribe)
                            .collect(Collectors.toSet());

                    for (String jobName : bot.botJobNameList()) {
                        if (existJobs.contains(jobName)) continue;
                        CommandMenuNode typeInput = new CommandMenuNode(true, null, jobName,
                                () -> JSON.toJSONString(bot.startBotJob(jobName))
                        );

                        typeInput.setTittleBuilder(() -> {
                            JobStatus status = null;
                            try {
                                status = getBot().getBotApi()
                                        .getBotJobService()
                                        .queryJobStatus(
                                                getBot().getScriptNodeName(),
                                                getBot().getAutoBotConfig().getBotKey(),
                                                jobName
                                        );
                                return "%s 任务 (%s)".formatted(jobName, status);
                            } catch (SchedulerException e) {
                                throw new RuntimeException(e);
                            }
                        });

                        node.addSubMenu(typeInput);
                    }
                    return "";
                }
        );
    }

    /**
     * 导入菜单节点
     *
     * @return CommandMenuNode
     */
    private CommandMenuNode buildImportMenuNode() {
        return new CommandMenuNode("导入", "请选择要导入的数据")
                .addSubMenu(buildImportBotAccountContextMenuNode());
    }

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
