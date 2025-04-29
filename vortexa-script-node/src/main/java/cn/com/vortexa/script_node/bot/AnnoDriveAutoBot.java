package cn.com.vortexa.script_node.bot;

import cn.com.vortexa.common.dto.ACListOptResult;
import cn.com.vortexa.common.dto.BotACJobResult;
import cn.com.vortexa.common.entity.BotInstance;
import cn.com.vortexa.common.entity.RewordInfo;
import cn.com.vortexa.script_node.anno.BotApplication;
import cn.com.vortexa.script_node.anno.BotMethod;
import cn.com.vortexa.script_node.dto.AccountRewordSnapshot;
import cn.com.vortexa.script_node.websocket.AccountWSClientBuilder;
import cn.com.vortexa.script_node.websocket.BaseBotWSClient;
import cn.com.vortexa.script_node.websocket.WebSocketClientLauncher;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.common.entity.BotInfo;
import cn.com.vortexa.common.exception.BotMethodFormatException;
import cn.com.vortexa.common.exception.BotMethodInvokeException;
import cn.com.vortexa.common.exception.BotInitException;
import cn.com.vortexa.script_node.dto.job.AutoBotJobRuntimeParam;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

import static cn.com.vortexa.common.entity.BotInfo.ACCOUNT_PARAMS_KEY;
import static cn.com.vortexa.common.entity.BotInfo.CONFIG_PARAMS_KEY;

@Slf4j
public abstract class AnnoDriveAutoBot<T extends JobInvokeAutoBot> extends JobInvokeAutoBot {

    /**
     * ws client 启动器
     */
    private final WebSocketClientLauncher webSocketClientLauncher = new WebSocketClientLauncher(this);

    /**
     * 奖励更新方法
     */
    @Getter
    private Method updateRewordMethod;

    /**
     * 奖励更新的代理方法
     */
    private final Method updateRewordProxyMethod;

    public AnnoDriveAutoBot() {
        try {
            updateRewordProxyMethod = AnnoDriveAutoBot.class
                    .getDeclaredMethod("rewardQueryProxyMethod", AccountContext.class, List.class);
            updateRewordProxyMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("reward query proxy method id empty", e);
        }
    }

    /**
     * 构建bot info， 会解析注解查询db，给上层父类调用
     *
     * @return BotInfo
     */
    @Override
    protected BotInfo buildBotInfo() throws BotInitException {
        // 解析bot 基本信息
        try {
            BotApplication annotation = getInstance().getClass().getAnnotation(BotApplication.class);

            if (annotation != null) {
                String botName = annotation.name();
                if (StrUtil.isBlank(botName)) {
                    throw new IllegalArgumentException("bot name 不能为空");
                }

                //  解析bot 自定义配置, 看是否有满足的
                AutoBotConfig botConfig = getAutoBotConfig();
                Map<String, Object> customConfig = botConfig.getCustomConfig();
                for (String key : annotation.configParams()) {
                    if (!customConfig.containsKey(key)) {
                        throw new IllegalArgumentException("缺乏Bot必要参数[%s]".formatted(key));
                    }
                }

                BotInfo botInfo = null;

                // 查询是否存在botKey的bot
                Map<String, Object> query = new HashMap<>();
                query.put("name", botName);
                List<BotInfo> dbBotInfoList = getBotApi().getBotInfoRPC().conditionQueryRPC(query);

                // 查询bot是否存在，不存在则创建
                if (dbBotInfoList == null || dbBotInfoList.isEmpty()) {
                    logger.warn("不存在bot info, 自动创建...");
                    botInfo = generateFromAnno(annotation);
                } else {
                    botInfo = dbBotInfoList.getFirst();
                    botInfo.fixMapValueType();
                }
                return botInfo;
            } else {
                throw new IllegalArgumentException("bot 应该带有 @BotApplication注解");
            }
        } catch (Exception e) {
            throw new BotInitException(e);
        }
    }

    @Override
    protected void resolveBotJobMethod() {
        // 解析bot job 参数
        logger.debug("resole job method...");
        // Step 1 遍历方法
        for (Method method : allMethods()) {
            method.setAccessible(true);

            // Step 2 找到方法中带有BotMethod注解的
            if (method.isAnnotationPresent(BotMethod.class)) {
                BotMethod botJobMethod = method.getAnnotation(BotMethod.class);

                // Step 3 根据BotMethod注解 的jobType，方法分类
                switch (botJobMethod.jobType()) {
                    case QUERY_REWARD -> queryRewardMethodHandler(method, botJobMethod);
                    case ONCE_TASK, TIMED_TASK -> timedTaskMethodHandler(method, botJobMethod);
                    case WEB_SOCKET_CONNECT -> webSocketConnectMethodHandler(method, botJobMethod);
                }
            }
        }
    }

    @Override
    protected void doStop() {
        updateRewordMethod = null;
        webSocketClientLauncher.clear();
        super.doStop();
    }

    @Override
    public Set<String> botJobNameList() {
        return getJobParams().keySet();
    }

    protected abstract T getInstance();

    /**
     * 保存并上传奖励信息
     *
     * @param aCListOptResult aCListOptResult
     */
    private void saveAndUploadRewordQueryResult(ACListOptResult aCListOptResult) {
        List<BotACJobResult> results = aCListOptResult.getResults();

        List<RewordInfo> rewordInfos = new ArrayList<>();
        for (BotACJobResult result : results) {
            if (result.getData() instanceof RewordInfo rewordInfo) {
                rewordInfos.add(rewordInfo);
            }
        }

        BotInstance botInstance = getBotInstance();
        getBotApi().getRewordInfoService().saveBatch(rewordInfos);
        getBotApi().getRewordInfoRPC().saveBatchRPC(
                botInstance.getBotId(),
                botInstance.getBotKey(),
                rewordInfos
        );
    }

    /**
     * 奖励查询方法处理器
     *
     * @param method       method
     * @param botJobMethod botJobMethod
     */
    private void queryRewardMethodHandler(Method method, BotMethod botJobMethod) {
        logger.debug("add reword query method");
        if (method.getReturnType() == AccountRewordSnapshot.class
                && method.getParameterCount() == 2
                && method.getParameters()[0].getType() == AccountContext.class
                && method.getParameters()[1].getType() == List.class
        ) {

            if (this.updateRewordMethod == null) {
                this.updateRewordMethod = method;
            } else {
                throw new BotMethodFormatException("收益查询方法只能有一个");
            }

            String updateRewordJobName = registryJobInBot(
                    getInstance(),
                    updateRewordMethod,
                    null,
                    botJobMethod
            );
            // 替换奖励查询方法为代理方法
            getJobRuntimeParamMap().get(updateRewordJobName).setMethod(updateRewordProxyMethod);
            addJobExecuteResultHandler(updateRewordJobName, this::saveAndUploadRewordQueryResult);
        } else {
            throw new BotMethodFormatException("收益查询方法错误, " +
                    "应为 AccountRewordSnapshot methodName(AccountContext ac, List<AccountContext> sameAccountBaseInfoIdLists)");
        }
    }

    /**
     * 奖励查询方法的代理方法，得到收益快照后存人数据库
     *
     * @param accountContext  accountContext
     * @param accountContexts accountContexts
     * @return AccountRewordSnapshot
     */
    private RewordInfo rewardQueryProxyMethod(AccountContext accountContext, List<AccountContext> accountContexts) {
        if (this.updateRewordMethod == null) {
            throw new BotMethodInvokeException("update reword into,,");
        } else {
            try {
                AccountRewordSnapshot rewordSnapshot = (AccountRewordSnapshot) this.updateRewordMethod.invoke(
                        this, accountContext, accountContexts
                );

                // 解析后存db
                return RewordInfo.builder()
                        .botId(getBotInfo().getId())
                        .botKey(getBotInstance().getBotKey())
                        .botAccountId(accountContext.getId())
                        .totalPoints(rewordSnapshot.getTotalPoints())
                        .dailyPoints(rewordSnapshot.getDailyPoints())
                        .build();
            } catch (Exception e) {
                throw new BotMethodInvokeException("reword query method invoke error", e);
            }
        }
    }

    /**
     * 定时任务方法处理器
     *
     * @param method       method
     * @param botJobMethod botJobMethod
     */
    private void timedTaskMethodHandler(Method method, BotMethod botJobMethod) {
        logger.debug("add [%s] method".formatted(botJobMethod));

        if (method.getParameterCount() > 2
                || method.getParameterCount() < 1
                || method.getParameters()[0].getType() != AccountContext.class
                || (method.getParameterCount() == 2 && method.getParameters()[1].getType() != List.class)
        ) {
            throw new BotMethodFormatException("定时任务方法错误, " +
                    "应为 void methodName(AccountContext ac) 或 " +
                    "void methodName(AccountContext exampleAC, List<AccountContext> sameABIIdList) ");
        }

        registryJobInBot(
                getInstance(),
                method,
                null,
                botJobMethod
        );
    }

    /**
     * Web socket 方法处理器
     *
     * @param method           method
     * @param botJobMethodAnno botJobMethodAnno
     */
    private void webSocketConnectMethodHandler(Method method, BotMethod botJobMethodAnno) {
        logger.debug("add ws [%s] method".formatted(botJobMethodAnno));
        Class<?> returnType = method.getReturnType();

        // 检查方法是否符合要求
        if (cn.com.vortexa.script_node.websocket.BaseBotWSClient.class.isAssignableFrom(returnType)
                && method.getParameterCount() == 1
                && method.getParameters()[0].getType() == AccountContext.class
        ) {
            try {
                // 符合要求，添加到jobMap
                String jobName = registryJobInBot(
                        webSocketClientLauncher,
                        WebSocketClientLauncher.lanuchMethod,
                        null,
                        botJobMethodAnno
                );

                AutoBotJobRuntimeParam runtimeParam = getJobRuntimeParamMap().get(jobName);

                if (runtimeParam != null) {
                    // 添加额外参数
                    runtimeParam.setExtraParams(new Object[]{getJobParam(jobName), (AccountWSClientBuilder) accountContext -> {
                        Object invoke = method.invoke(getInstance(), accountContext);
                        return (BaseBotWSClient<?>) invoke;
                    }});
                }

            } catch (Exception e) {
                throw new BotMethodFormatException(e);
            }
        } else {
            throw new BotMethodFormatException(
                    "websocket 方法错误, 应为 BotWebSocketContext<?,?> methodName(AccountContext)");
        }
    }

    /**
     * 运行bot method
     *
     * @param method method
     * @param args   args
     * @return CompletableFuture<R>
     */
    private @NotNull CompletableFuture<Result> invokeBotMethod(Method method, Object... args) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return (Result) method.invoke(this, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new BotMethodInvokeException(String.format(
                        "执行[%s]-[%s]方法发生异常",
                        getBotInstance().getBotKey(),
                        method.getName()
                ), e);
            }
        }, getExecutorService());
    }

    protected BotInfo generateFromAnno(BotApplication annotation) {
        BotInfo botInfo = new BotInfo();
        botInfo.setName(annotation.name());
        botInfo.setDescription(annotation.describe());
        botInfo.setImage(annotation.image());
        botInfo.setLimitProjectIds(Arrays.toString(annotation.limitProjectIds()));
        botInfo.getParams()
                .put(CONFIG_PARAMS_KEY, JSONArray.parseArray(JSONArray.toJSONString(annotation.configParams())));
        botInfo.getParams()
                .put(ACCOUNT_PARAMS_KEY, JSONArray.parseArray(JSONArray.toJSONString(annotation.accountParams())));

        return botInfo;
    }

    public List<Method> allMethods() {
        List<Method> list = new ArrayList<>();
        Class<?> clazz = getClass();

        while (clazz != null) {
            // 获取当前类的所有声明的方法，包括私有方法、保护方法和公共方法
            Method[] methods = clazz.getDeclaredMethods();
            list.addAll(Arrays.asList(methods));
            // 获取父类继续遍历
            clazz = clazz.getSuperclass();
        }
        return list;
    }
}
