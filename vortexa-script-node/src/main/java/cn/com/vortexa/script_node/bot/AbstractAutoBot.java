package cn.com.vortexa.script_node.bot;

import cn.com.vortexa.script_node.config.ScriptNodeConfiguration;
import cn.com.vortexa.script_node.service.BotApi;
import cn.com.vortexa.common.constants.BotStatus;
import cn.com.vortexa.common.constants.HttpMethod;
import cn.com.vortexa.common.dto.job.AutoBotJobParam;
import cn.com.vortexa.common.util.log.AppendLogger;
import cn.com.vortexa.common.dto.AutoBotRuntimeInfo;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.entity.BotInfo;
import cn.com.vortexa.common.entity.BotInstance;
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

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

public abstract class AbstractAutoBot {

    private static final ProxyInfo DEFAULT_PROXY = new ProxyInfo();

    public AppendLogger logger;

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

    @Getter
    private BotInstance botInstance;

    /**
     * script node配置
     */
    @Getter
    private ScriptNodeConfiguration scriptNodeConfiguration;

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
    public final void init(
            ScriptNodeConfiguration scriptNodeConfiguration, BotApi botApi, AutoBotConfig autoBotConfig
    ) throws BotInitException {
        try {
            this.logger = new AppendLogger(
                    scriptNodeConfiguration.getScriptNodeName(),
                    autoBotConfig.getBotName(),
                    autoBotConfig.getBotKey()
            );
        } catch (IOException e) {
            throw new BotInitException("bot logger create error", e);
        }

        this.scriptNodeConfiguration = scriptNodeConfiguration;
        this.botApi = botApi;

        // Step 1 参数校验
        if (StrUtil.isBlank(autoBotConfig.getBotKey())) {
            throw new IllegalArgumentException("bot key is empty!");
        }

        // Step 2 进入INIT状态，获取参数
        updateState(BotStatus.INIT);
        this.autoBotConfig = autoBotConfig;
        try {
            // Step 2.1 获取BotInfo,解析Bot Job
            botInfo = buildBotInfo();
            botInfo.setVersionCode(autoBotConfig.getMetaInfo().getVersion_code());

            // 获取方法上的job配置，判断是否有变化，有变化需要发到远程更新
            Map<String, AutoBotJobParam> remoteBotJobParamMap = botInfo.getJobParams();
            Map<String, AutoBotJobParam> classJobParamMap = resolveBotJobMethod();

            if (judgeClassJobParamChanged(remoteBotJobParamMap, classJobParamMap)) {
                mergeBotInfoJobParamMap(remoteBotJobParamMap, classJobParamMap);

                botApi.getBotInfoRPC().insertOrUpdateRPC(botInfo);
            }
        } catch (Exception e) {
            throw new BotInitException("resolve bot job error", e);
        }

        // Step 2.2 构建BotInstance
        this.botInstance = BotInstance.builder()
                .botId(botInfo.getId())
                .botKey(autoBotConfig.getBotKey())
                .botName(botInfo.getName())
                .scriptNodeName(scriptNodeConfiguration.getScriptNodeName())
                .jobParams(botInfo.getJobParams())
                .params(botInfo.getParams())
                .versionCode(botInfo.getVersionCode())
                .build();

        // Step 2.3 设置logger前缀与线程池
        this.executorService = Executors.newThreadPerTaskExecutor(
                new NamedThreadFactory(botInfo.getName() + "-executor"));

        try {
            // Step 2.4 初始化存储的Table
            initDBTable(botApi);

            // Step 2.5 初始化BotInstance实例
            logger.info("start init bot instance");

            BotInstance dbInstance = getBotApi().getBotInstanceRPC().selectOneRPC(
                    BotInstance.builder().scriptNodeName(botInstance.getScriptNodeName()).botKey(botInstance.getBotKey()).build()
            );
            // 数据库存在bot instance实例信息合并数据库的。
            if (dbInstance != null) {
                Map<String, AutoBotJobParam> clsssJobParamMap = botInstance.getJobParams();
                Map<String, AutoBotJobParam> remoteJobParamMap = dbInstance.getJobParams();
                for (Map.Entry<String, AutoBotJobParam> entry : clsssJobParamMap.entrySet()) {
                    String jobName = entry.getKey();
                    AutoBotJobParam remoteJobParam = remoteJobParamMap.get(jobName);
                    if (remoteJobParamMap.containsKey(jobName)) {
                        entry.getValue().merge(remoteJobParam);
                    }
                }
                logger.info("exist db botInstance, marge exist instance config...");
            }

            logger.info("start save bot instance...");
            if (botApi.getBotInstanceRPC().insertOrUpdateRPC(botInstance) > 0) {
                logger.info("bot instance save success...");
            } else {
                throw new BotInitException("bot instance save error");
            }

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
     * 停止Bot
     */
    public void stop() {
        updateState(BotStatus.STOPPING);
        try {
            networkSyncControllerMap.clear();
            doStop();
            updateState(BotStatus.STOPPED);
        } catch (Exception e) {
            logger.error("stop bot error, " + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
            updateState(BotStatus.SHUTDOWN);
        }
    }

    protected abstract void doStop();

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
    public CompletableFuture<JSONObject> syncJSONRequest(
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
        ).thenApply(responseStr -> {
            if (responseStr == null) return null;
            return JSONObject.parseObject(responseStr);
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
                logger.debug("同步器允许发送请求 - " + str);

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
        return FileUtil.getAppResourceAppConfigDir() + File.separator + getBotInstance().getBotKey();
    }

    /**
     * 更新BotStatus
     *
     * @param newStatus 新状态
     */
    public synchronized void updateState(BotStatus newStatus) throws BotStatusException {

        boolean b = switch (status) {
            //当前为NEW，新状态才能为NEW,SHUTDOWN
            case NOT_LOADED -> BotStatus.NEW.equals(newStatus);
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
            case RUNNING -> newStatus.equals(BotStatus.STOPPING);
            case STOPPING -> newStatus.equals(BotStatus.STOPPED) || newStatus.equals(BotStatus.SHUTDOWN);
            case STOPPED -> newStatus.equals(BotStatus.INIT);
            case SHUTDOWN -> throw new BotStatusException("bot already shutdown");
        };

        if (b) {
            logger.debug("Status change [%s] => [%s]".formatted(status, newStatus));
            this.status = newStatus;
        } else {
            throw new BotStatusException(
                    String.format("%s status can't from[%s] -> to[%s]", runtimeBotName(), status, newStatus));
        }
    }

    public String runtimeBotName() {
        return "Bot[%s]-[%s]".formatted(botInstance.getBotName(), autoBotConfig.getBotKey());
    }

    public String getScriptNodeName() {
        return scriptNodeConfiguration.getScriptNodeName();
    }

    protected synchronized Map<String, AutoBotJobParam> getJobParams() {
        return this.botInstance == null ? new HashMap<>() : this.botInstance.getJobParams();
    }

    protected synchronized AutoBotJobParam getJobParam(String jobName) {
        return this.botInstance.getJobParams() == null ? null : this.botInstance.getJobParams().get(jobName);
    }

    protected synchronized void setJobParam(String jobKey, AutoBotJobParam jobParam) {
        Map<String, AutoBotJobParam> jobParams = this.botInfo.getJobParams();
        jobParams.compute(jobKey, (k, v) -> {
            if (v == null) {
                return jobParam;
            } else {
                v.merge(jobParam);
                return v;
            }
        });
        jobParams.put(jobKey, jobParam);
    }

    protected abstract BotInfo buildBotInfo() throws BotInitException;

    protected abstract Map<String, AutoBotJobParam> resolveBotJobMethod();


    /**
     * 判断jar包内的job param与远程配置相比是否发生变化
     *
     * @param remoteBotJobParamMap remoteBotJobParamMap
     * @param classBotJobParamMap  classBotJobParamMap
     * @return 是否有变化
     */
    private boolean judgeClassJobParamChanged(
            Map<String, AutoBotJobParam> remoteBotJobParamMap, Map<String, AutoBotJobParam> classBotJobParamMap
    ) {
        if (remoteBotJobParamMap.size() != classBotJobParamMap.size()) return true;
        if (!remoteBotJobParamMap.keySet().containsAll(classBotJobParamMap.keySet())) {
            return true;
        }

        for (Map.Entry<String, AutoBotJobParam> entry : remoteBotJobParamMap.entrySet()) {
            String jobName = entry.getKey();
            AutoBotJobParam remoteJobParam = entry.getValue();
            AutoBotJobParam classJobParam = classBotJobParamMap.get(jobName);
            if (remoteJobParam.equals(classJobParam)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 初始化数据库表
     *
     * @param botApi botApi
     * @throws SQLException SQLException
     */
    private void initDBTable(BotApi botApi) throws SQLException {
        logger.info("start init database table. %s-%s".formatted(botInfo.getId(), getAutoBotConfig().getBotKey()));
        // 检查对应分表是否存在
        if (!botApi.getBotAccountService()
                .checkAndCreateShardedTable(botInfo.getId(), getAutoBotConfig().getBotKey(), true)) {
            throw new RuntimeException("bot account table create error");
        }
        if (!botApi.getRewordInfoService().checkAndCreateShardedTable(botInfo.getId(), getAutoBotConfig().getBotKey())) {
            throw new RuntimeException("account reword table create error");
        }
        logger.info("database table init finish");
    }


    /**
     * 合并job param map
     *
     * @param oldBotJobParamMap oldBotJobParamMap
     * @param newJobParamMap     newJobParamMap
     */
    private static void mergeBotInfoJobParamMap(
            Map<String, AutoBotJobParam> oldBotJobParamMap, Map<String, AutoBotJobParam> newJobParamMap
    ) {
        Iterator<Map.Entry<String, AutoBotJobParam>> iterator = oldBotJobParamMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, AutoBotJobParam> entry = iterator.next();
            String jobName = entry.getKey();
            AutoBotJobParam remoteJobParam = entry.getValue();
            if (!newJobParamMap.containsKey(jobName)) {
                iterator.remove();
            } else if (remoteJobParam.getJobType().equals(newJobParamMap.get(jobName).getJobType())) {
                // job类型变化，需要整个更新
                entry.setValue(newJobParamMap.get(jobName));
                newJobParamMap.remove(jobName);
            }
        }
        oldBotJobParamMap.putAll(newJobParamMap);
    }
}
