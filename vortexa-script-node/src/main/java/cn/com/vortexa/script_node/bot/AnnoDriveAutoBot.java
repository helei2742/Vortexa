package cn.com.vortexa.script_node.bot;

import cn.com.vortexa.script_node.anno.BotApplication;
import cn.com.vortexa.script_node.anno.BotMethod;
import cn.com.vortexa.script_node.websocket.AccountWSClientBuilder;
import cn.com.vortexa.script_node.websocket.BaseBotWSClient;
import cn.com.vortexa.script_node.websocket.WebSocketClientLauncher;
import cn.com.vortexa.common.constants.BotJobType;
import cn.com.vortexa.script_node.constants.MapConfigKey;
import cn.com.vortexa.common.dto.ACListOptResult;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.common.entity.BotInfo;
import cn.com.vortexa.common.exception.BotMethodFormatException;
import cn.com.vortexa.common.exception.BotMethodInvokeException;
import cn.com.vortexa.common.exception.BotInitException;
import cn.com.vortexa.script_node.dto.job.AutoBotJobRuntimeParam;
import cn.hutool.core.util.BooleanUtil;
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
     * 注册方法
     */
    @Getter
    private Method registerMethod;

    /**
     * 登录方法
     */
    @Getter
    private Method loginMethod;

    /**
     * 奖励更新方法
     */
    @Getter
    private Method updateRewordMethod;

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

        // Step 1 遍历方法
        for (Method method : getClass().getDeclaredMethods()) {
            method.setAccessible(true);

            // Step 2 找到方法中带有BotMethod注解的
            if (method.isAnnotationPresent(BotMethod.class)) {
                BotMethod botJobMethod = method.getAnnotation(BotMethod.class);

                // Step 3 根据BotMethod注解 的jobType，方法分类
                switch (botJobMethod.jobType()) {
                    case REGISTER -> registerMethodHandler(method, botJobMethod);
                    case LOGIN -> loginMethodHandler(method, botJobMethod);
                    case QUERY_REWARD -> queryRewardMethodHandler(method, botJobMethod);
                    case ONCE_TASK, TIMED_TASK -> timedTaskMethodHandler(method, botJobMethod);
                    case WEB_SOCKET_CONNECT -> webSocketConnectMethodHandler(method, botJobMethod);
                }
            }
        }
    }

    /**
     * 注册type账号
     *
     * @return String
     */
    @Override
    public CompletableFuture<ACListOptResult> registerAccount() {
        if (registerMethod == null) {
            return CompletableFuture.completedFuture(ACListOptResult.fail(
                    getBotInstance().getBotId(),
                    getBotInstance().getBotName(),
                    BotJobType.REGISTER.name(),
                    "未找到注册方法"
            ));
        }
        return uniqueAsyncForACList(
                (accountContext, accountContexts) -> {
                    if (BooleanUtil.isTrue(accountContext.isSignUp())) {
                        // 账户注册过，
                        String errorMsg = String.format("[%s]账户[%s]-email[%s]注册过", accountContext.getId(),
                                accountContext.getName(),
                                accountContext.getAccountBaseInfo().getEmail());

                        log.warn(errorMsg);

                        return CompletableFuture.completedFuture(Result.fail(errorMsg));
                    } else if (registerMethod != null) {
                        // 调用注册方法注册
                        return invokeBotMethod(
                                registerMethod,
                                accountContext,
                                accountContexts,
                                getAutoBotConfig().getConfig(MapConfigKey.INVITE_CODE_KEY)
                        ).thenApplyAsync(result -> {
                            if (result.getSuccess()) {
                                for (AccountContext ac : accountContexts) {
                                    AccountContext.signUpSuccess(ac);
                                }
                            }
                            return result;
                        });
                    } else {
                        return CompletableFuture.completedFuture(Result.fail("未知错误"));
                    }
                },
                (accountContext, result) -> {
                    // 登录成功
                    if (BooleanUtil.isTrue(result.getSuccess())) {
                        //注册成功
                        AccountContext.signUpSuccess(accountContext);
                    }
                    return result;
                },
                BotJobType.REGISTER.name()
        );
    }

    /**
     * 获取账号的token
     *
     * @return String
     */
    @Override
    public CompletableFuture<ACListOptResult> loginAndTakeTokenAccount() {
        if (loginMethod == null) {
            return CompletableFuture.completedFuture(ACListOptResult.fail(
                    getBotInstance().getBotId(),
                    getBotInstance().getBotName(),
                    BotJobType.LOGIN.name(),
                    "未找到登录方法"
            ));
        }

        return asyncForACList(
                accountContext -> invokeBotMethod(
                        loginMethod,
                        accountContext
                ),
                (accountContext, result) -> {
                    // 登录成功
                    if (BooleanUtil.isTrue(result.getSuccess())) {
                        String token = result.getData() == null ? null :
                                (result.getData() instanceof String ? (String) result.getData() : null);

                        // token不为空，设置到accountContext里
                        if (StrUtil.isNotBlank(token)) {
                            accountContext.setParam(MapConfigKey.TOKEN_KEY, token);
                        } else {
                            log.debug("账号[{}]-[{}]token为空", accountContext.getId(), accountContext.getName());
                        }
                    }
                    return result;
                },
                BotJobType.LOGIN.name()
        );
    }

    @Override
    public CompletableFuture<ACListOptResult> updateAccountRewordInfo() {
        if (updateRewordMethod == null) {
            return CompletableFuture.completedFuture(ACListOptResult.fail(
                    getBotInstance().getBotId(),
                    getBotInstance().getBotName(),
                    BotJobType.QUERY_REWARD.name(),
                    "未找到奖励查询方法"
            ));
        }

        return asyncForACList(
                getUniqueACList(),
                accountContext -> invokeBotMethod(
                        updateRewordMethod,
                        accountContext,
                        getAcMap().get(accountContext.getAccountBaseInfoId())
                ),
                (accountContext, result) -> result,
                BotJobType.QUERY_REWARD.name()
        );
    }

    @Override
    public Set<String> botJobNameList() {
        return getJobParams().keySet();
    }

    protected abstract T getInstance();

    /**
     * 注册方法处理器
     *
     * @param method       method
     * @param botJobMethod botJobMethod
     */
    private void registerMethodHandler(Method method, BotMethod botJobMethod) {
        logger.debug("add register method");
        if (method.getReturnType() == Result.class
                && method.getParameterCount() == 3
                && method.getParameters()[0].getType() == AccountContext.class
                && method.getParameters()[1].getType() == List.class
                && method.getParameters()[2].getType() == String.class) {

            if (this.registerMethod == null) {
                this.registerMethod = method;
                this.addBasicJob(BotJobType.REGISTER);

                registryJobInBot(
                        getInstance(),
                        method,
                        null,
                        botJobMethod
                );
            } else {
                throw new BotMethodFormatException("注册方法只能有一个");
            }
        } else {
            throw new BotMethodFormatException("注册方法错误, " +
                    "应为 Result methodName(AccountContext accountContext, List<AccountContext> sameAccountBaseInfoIdLists, String inviteCode)");
        }
    }

    /**
     * 登录方法处理器
     *
     * @param method       method
     * @param botJobMethod botJobMethod
     */
    private void loginMethodHandler(Method method, BotMethod botJobMethod) {
        logger.debug("add login method");
        if (method.getReturnType() == Result.class
                && method.getParameterCount() == 1
                && method.getParameters()[0].getType() == AccountContext.class
        ) {

            if (this.loginMethod == null) {
                this.loginMethod = method;
                this.addBasicJob(BotJobType.LOGIN);

                registryJobInBot(
                        getInstance(),
                        method,
                        null,
                        botJobMethod
                );
            } else {
                throw new BotMethodFormatException("登录方法只能有一个");
            }
        } else {
            throw new BotMethodFormatException("登录方法错误, " +
                    "应为 Result methodName(AccountContext ac");
        }
    }

    /**
     * 奖励查询方法处理器
     *
     * @param method       method
     * @param botJobMethod botJobMethod
     */
    private void queryRewardMethodHandler(Method method, BotMethod botJobMethod) {
        logger.debug("add reword query method");
        if (method.getReturnType() == Result.class
                && method.getParameterCount() == 2
                && method.getParameters()[0].getType() == AccountContext.class
                && method.getParameters()[1].getType() == List.class
        ) {

            if (this.updateRewordMethod == null) {
                this.updateRewordMethod = method;
            } else {
                throw new BotMethodFormatException("收益查询方法只能有一个");
            }

            registryJobInBot(
                    getInstance(),
                    method,
                    null,
                    botJobMethod
            );
        } else {
            throw new BotMethodFormatException("收益查询方法错误, " +
                    "应为 Result methodName(AccountContext ac, List<AccountContext> sameAccountBaseInfoIdLists)");
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
        botInfo.setDescribe(annotation.describe());
        botInfo.setImage(annotation.image());
        botInfo.setLimitProjectIds(Arrays.toString(annotation.limitProjectIds()));
        botInfo.getParams()
                .put(CONFIG_PARAMS_KEY, JSONArray.parseArray(JSONArray.toJSONString(annotation.configParams())));
        botInfo.getParams()
                .put(ACCOUNT_PARAMS_KEY, JSONArray.parseArray(JSONArray.toJSONString(annotation.accountParams())));

        return botInfo;
    }
}
