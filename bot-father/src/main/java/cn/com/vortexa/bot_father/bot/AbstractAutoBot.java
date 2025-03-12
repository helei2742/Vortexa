package cn.com.vortexa.bot_father.bot;

import cn.com.vortexa.bot_father.service.BotApi;
import cn.com.vortexa.bot_father.constants.BotStatus;
import cn.com.vortexa.common.config.SystemConfig;
import cn.com.vortexa.common.constants.HttpMethod;
import cn.com.vortexa.common.dto.job.AutoBotJobParam;
import cn.com.vortexa.bot_father.util.log.AppendLogger;
import cn.com.vortexa.common.dto.AutoBotRuntimeInfo;
import cn.com.vortexa.bot_father.config.AutoBotConfig;
import cn.com.vortexa.common.entity.BotInfo;
import cn.com.vortexa.common.entity.ProxyInfo;
import cn.com.vortexa.common.exception.BotInitException;
import cn.com.vortexa.common.exception.BotStatusException;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.NamedThreadFactory;
import cn.com.vortexa.common.util.http.RestApiClientFactory;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;


public abstract class AbstractAutoBot {

    private static final ProxyInfo DEFAULT_PROXY = new ProxyInfo();

    public final AppendLogger logger = new AppendLogger(getClass());

    /**
     * 代理并发控制
     */
    private final Map<ProxyInfo, Semaphore> networkSyncControllerMap;

    /**
     * bot api
     */
    @Getter
    private BotApi botApi;

    /**
     * bot运行时信息
     */
    @Getter
    private final AutoBotRuntimeInfo autoBotRuntimeInfo;

    /**
     * 请求并发数量
     */
    @Getter
    @Setter
    private int requestConcurrentCount = 5;

    /**
     * bot信息
     */
    @Getter
    private BotInfo botInfo;

    /**
     * 配置
     */
    @Getter
    private AutoBotConfig autoBotConfig;

    /**
     * 执行异步任务的线程池
     */
    @Getter
    private ExecutorService executorService;

    /**
     * 状态
     */
    @Getter
    private BotStatus status = BotStatus.NEW;

    public AbstractAutoBot() {
        this.networkSyncControllerMap = new ConcurrentHashMap<>();
        this.autoBotRuntimeInfo = new AutoBotRuntimeInfo();
    }

    /**
     * 初始化方法
     *
     * @param autoBotConfig autoBotConfig
     * @throws BotInitException BotInitException
     */
    public final void init(BotApi botApi, AutoBotConfig autoBotConfig) throws BotInitException {
        this.botApi = botApi;

        // Step 1 参数校验
        if (StrUtil.isBlank(autoBotConfig.getBotKey())) {
            throw new IllegalArgumentException("bot key is empty!");
        }

        // Step 2 进入INIT状态，获取参数
        updateState(BotStatus.INIT);
        this.autoBotConfig = autoBotConfig;

        // Step 2.1 获取BotInfo,解析Bot Job
        this.botInfo = buildBotInfo();

        resolveBotJobMethod();

        // Step 2.2 保存bot info
        try {
            if (botApi.getBotInfoRPC().insertOrUpdate(botInfo) == 1) {
                Map<String, Object> query = new HashMap<>();
                query.put("name", botInfo.getName());
                // 保存成还需查询botId
                Integer id = botApi.getBotInfoRPC().conditionQuery(query).getFirst().getId();
                botInfo.setId(id);
                AutoBotConfig.BOT_ID = id;
                logger.info("save bot info success, id:" + botInfo.getId());
            }
        } catch (SQLException e) {
            throw new BotInitException("save bot info error", e);
        }

        // Step 2.3 设置logger前缀与线程池
        String botName = runtimeBotName();
        logger.append(botName);
        this.executorService = Executors.newThreadPerTaskExecutor(new NamedThreadFactory(botInfo.getName() + "-executor"));

        try {
            // Step 2.4 初始化存储的Table
            logger.info("start init database table");
            // 检查对应分表是否存在
            if (!botApi.getBotAccountRPC().checkAndCreateShardedTable(botInfo.getId(), getAutoBotConfig().getBotKey(), true)) {
                throw new RuntimeException("bot account table create error");
            }
            logger.info("database table init finish");

            // Step 2.5 子类初始化
            doInit();

            // Step 2。6 更新状态
            updateState(BotStatus.INIT_FINISH);
        } catch (Exception e) {
            logger.error("init error", e);
            updateState(BotStatus.INIT_ERROR);
        }
    }


    /**
     * 初始化方法
     */
    protected abstract void doInit() throws BotInitException;

    /**
     * 同步请求，使用syncController控制并发
     *
     * @param proxy   proxy
     * @param url     url
     * @param method  method
     * @param headers headers
     * @param params  params
     * @param body    body
     * @return CompletableFuture<String> response str
     */
    public CompletableFuture<String> syncRequest(
            ProxyInfo proxy,
            String url,
            HttpMethod method,
            Map<String, String> headers,
            JSONObject params,
            JSONObject body
    ) {
        return syncRequest(proxy, url, method, headers, params, body, 1);
    }

    /**
     * 同步请求，使用syncController控制并发
     *
     * @param proxy   proxy
     * @param url     url
     * @param method  method
     * @param headers headers
     * @param params  params
     * @param body    body
     * @return CompletableFuture<String> response str
     */
    public CompletableFuture<String> syncRequest(
            ProxyInfo proxy,
            String url,
            HttpMethod method,
            Map<String, String> headers,
            JSONObject params,
            JSONObject body,
            int retryTimes
    ) {
        return syncRequest(proxy, url, method, headers, params, body, null, retryTimes);
    }

    /**
     * 同步请求，使用syncController控制并发
     *
     * @param proxy   proxy
     * @param url     url
     * @param method  method
     * @param headers headers
     * @param params  params
     * @param body    body
     * @return CompletableFuture<Response> String
     */
    public CompletableFuture<String> syncRequest(
            ProxyInfo proxy,
            String url,
            HttpMethod method,
            Map<String, String> headers,
            JSONObject params,
            JSONObject body,
            Supplier<String> requestStart
    ) {
        return syncRequest(
                proxy,
                url,
                method,
                headers,
                params,
                body,
                requestStart,
                1
        );
    }


    /**
     * 同步请求，使用syncController控制并发
     *
     * @param proxy   proxy
     * @param url     url
     * @param method  method
     * @param headers headers
     * @param params  params
     * @param body    body
     * @return CompletableFuture<Response> String
     */
    public CompletableFuture<String> syncRequest(
            ProxyInfo proxy,
            String url,
            HttpMethod method,
            Map<String, String> headers,
            JSONObject params,
            JSONObject body,
            Supplier<String> requestStart,
            int retryTimes
    ) {
        return syncCCHandler(proxy, requestStart, () -> {
            try {
                return RestApiClientFactory.getClient(proxy).request(
                        url,
                        method,
                        headers,
                        params,
                        body,
                        retryTimes
                ).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 同步请求，使用syncController控制并发
     *
     * @param proxy   proxy
     * @param url     url
     * @param method  method
     * @param headers headers
     * @param params  params
     * @param body    body
     * @return CompletableFuture<Response> String
     */
    public CompletableFuture<List<String>> syncStreamRequest(
            ProxyInfo proxy,
            String url,
            HttpMethod method,
            Map<String, String> headers,
            JSONObject params,
            JSONObject body,
            Supplier<String> requestStart
    ) {
        return syncCCHandler(proxy, requestStart, () -> {
            try {
                return RestApiClientFactory.getClient(proxy).streamRequest(
                        url,
                        method,
                        headers,
                        params,
                        body
                ).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <R> CompletableFuture<R> syncCCHandler(
            ProxyInfo proxy,
            Supplier<String> requestStart,
            Supplier<R> request
    ) {

        Semaphore networkController = networkSyncControllerMap
                .compute(proxy == null ? DEFAULT_PROXY : proxy, (k, v) -> {
                    if (v == null) {
                        v = new Semaphore(requestConcurrentCount);
                    }
                    return v;
                });


        return CompletableFuture.supplyAsync(() -> {
            try {
                networkController.acquire();
                // 随机延迟
                TimeUnit.MILLISECONDS.sleep(RandomUtil.randomLong(1000, 3000));

                String str = "start network request";
                if (requestStart != null) {
                    str = requestStart.get();
                }
                logger.info("同步器允许发送请求 - " + str);

                return request.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                networkController.release();
            }
        }, executorService);
    }

    /**
     * 打印BotRuntimeInfo
     *
     * @return string
     */
    public String printBotRuntimeInfo() {
        StringBuilder sb = new StringBuilder();
        getAutoBotRuntimeInfo().getKeyValueInfoMap().forEach((k, v) -> {
            sb.append(k).append(": ").append(v).append("\n");
        });
        return sb.toString();
    }

    /**
     * 获取app的配置目录
     *
     * @return String
     */
    public String getAppConfigDir() {
        return FileUtil.getConfigDirResourcePath(SystemConfig.CONFIG_DIR_APP_PATH, botInfo.getName());
    }


    /**
     * 更新BotStatus
     *
     * @param newStatus 新状态
     */
    public synchronized void updateState(BotStatus newStatus) throws BotStatusException {
        boolean b = true;
        if (newStatus.equals(BotStatus.SHUTDOWN)) {
            status = BotStatus.SHUTDOWN;
            b = false;
        } else {
            b = switch (status) {
                //当前为NEW，新状态才能为NEW,SHUTDOWN
                case NEW -> BotStatus.INIT.equals(newStatus);
                //当前为INIT，新状态只能为INIT_FINISH、INIT_ERROR,SHUTDOWN
                case INIT -> newStatus.equals(BotStatus.INIT_FINISH)
                        || newStatus.equals(BotStatus.INIT_ERROR);
                //当前为INIT_ERROR,新状态只能为ACCOUNT_LOADING, SHUTDOWN
                case INIT_ERROR -> newStatus.equals(BotStatus.INIT);
                //当前状态为INIT_FINISH，新状态只能为ACCOUNT_LIST_CONNECT, SHUTDOWN
                case INIT_FINISH -> newStatus.equals(BotStatus.STARTING);
                //当前状态为STARING，新状态只能为RUNNING,SHUTDOWN
                case STARTING -> newStatus.equals(BotStatus.RUNNING);
                //RUNNING，新状态只能为 SHUTDOWN
                case RUNNING -> false;
                case SHUTDOWN -> false;
            };
        }


        if (b) {
            logger.info("Status change [%s] => [%s]".formatted(status, newStatus));
            this.status = newStatus;
        } else {
            throw new BotStatusException(String.format("%s status can't from[%s] -> to[%s]", runtimeBotName(), status, newStatus));
        }
    }


    public String runtimeBotName() {
        return "Bot[%s]-[%s]".formatted(botInfo.getName(), autoBotConfig.getBotKey());
    }

    protected synchronized Map<String, AutoBotJobParam> getJobParams() {
        return this.botInfo.getJobParams();
    }

    protected synchronized AutoBotJobParam getJobParam(String jobName) {
        return this.botInfo.getJobParams() == null ? null : this.botInfo.getJobParams().get(jobName);
    }

    protected synchronized void setJobParam(String jobKey, AutoBotJobParam jobParam) {
//        if (!this.botInfo.getJobParams().containsKey(jobKey)) {
        this.botInfo.getJobParams().put(jobKey, jobParam);
//        }
    }

    protected abstract BotInfo buildBotInfo() throws BotInitException;

    protected abstract void resolveBotJobMethod();
}
