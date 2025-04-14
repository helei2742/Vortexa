package cn.com.vortexa.script_node.bot;

import cn.com.vortexa.script_node.anno.BotMethod;
import cn.com.vortexa.script_node.anno.BotWSMethodConfig;
import cn.com.vortexa.common.constants.BotJobType;
import cn.com.vortexa.common.dto.ACListOptResult;
import cn.com.vortexa.common.dto.BotACJobResult;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.job.AutoBotJobParam;
import cn.com.vortexa.common.dto.job.AutoBotJobWSParam;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.job.core.AutoBotJobInvoker;
import cn.com.vortexa.script_node.dto.job.AutoBotJobRuntimeParam;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.quartz.CronExpression;
import org.quartz.JobKey;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


import static cn.com.vortexa.common.constants.BotJobType.ACCOUNT_SPLIT_JOB;
import static cn.com.vortexa.common.dto.job.AutoBotJobParam.START_AT;

@Getter
public abstract class JobInvokeAutoBot extends AccountManageAutoBot implements AutoBotJobInvoker {

    private final Map<String, AutoBotJobRuntimeParam> jobRuntimeParamMap = new ConcurrentHashMap<>();

    private final Random random = new Random();

    @Override
    public void invokeJob(JobKey jobKey, AutoBotJobParam jobParam) {
        String jobName = jobKey.getName();

        // Step 1 获取运行时参数
        AutoBotJobRuntimeParam runtimeParam = jobRuntimeParamMap.get(jobName);

        // 账户拆分任务,不需要遍历账户列表
        if (ACCOUNT_SPLIT_JOB.equals(jobParam.getJobType())) {
            runtimeParam = jobRuntimeParamMap.get(jobKey.getGroup());
            invokeAccountSplitJob(jobKey, jobParam, runtimeParam);
            return;
        }

        // 动态触发器
        // 遍历账户，生成新的一些一次性任务执行
        if (BooleanUtil.isTrue(jobParam.getDynamicTrigger())) {
            splitJobByAccount(jobParam, jobName);
            return;
        }

        // Step 2 调用执行方法
        logger.info("开始执行[%s]定时任务".formatted(jobName));

        CompletableFuture<ACListOptResult> future = null;
        if (BooleanUtil.isTrue(jobParam.getUniqueAccount())) {
            future = uniqueForEachAccount(runtimeParam, jobName, jobParam, this::uniqueInvoke);
        } else {
            future = normalForEachAccount(runtimeParam, jobName, jobParam, this::normalInvoke);
        }

        acListOptResultHandler(future);


        logger.info("[%s]定时任务执行完毕".formatted(jobName));
    }

    @Override
    protected void doStop() {
        jobRuntimeParamMap.clear();
        super.doStop();
    }

    /**
     * 将Job注册到Bot中
     *
     * @param method           方法
     * @param botJobMethodAnno 方法上的BotMethod注解
     */
    public String registryJobInBot(
            Object invokeObj,
            Method method,
            Object[] extraParams,
            BotMethod botJobMethodAnno
    ) {
        AutoBotJobParam autoBotJobParam = null;

        try {
            Integer intervalInSecond = null;
            CronExpression cronExpression = null;

            String jobName = StrUtil.isBlank(botJobMethodAnno.jobName()) ? method.getName() : botJobMethodAnno.jobName();

            // Step 1 判断合法性
            if (botJobMethodAnno.intervalInSecond() != 0) {
                intervalInSecond = botJobMethodAnno.intervalInSecond();
            } else if (StrUtil.isNotBlank(botJobMethodAnno.cronExpression())) {
                cronExpression = new CronExpression(botJobMethodAnno.cronExpression());
                logger.info("%s cronExpression:[%s]".formatted(jobName, cronExpression.getCronExpression()));
            } else if (
                    botJobMethodAnno.jobType() != BotJobType.ONCE_TASK
                            && botJobMethodAnno.jobType() != BotJobType.REGISTER
                            && botJobMethodAnno.jobType() != BotJobType.LOGIN
            ) {
                throw new IllegalArgumentException("定时任务需设置时间间隔或cron表达式");
            }

            // Step 2 设置job运行时参数
            getJobRuntimeParamMap().put(
                    jobName,
                    AutoBotJobRuntimeParam
                            .builder()
                            .target(invokeObj)
                            .method(method)
                            .extraParams(extraParams)
                            .build()
            );

            // Step 3 构建job task参数
            autoBotJobParam = AutoBotJobParam
                    .builder()
                    .jobName(jobName)
                    .jobType(botJobMethodAnno.jobType())
                    .description(botJobMethodAnno.description())
                    .cronExpression(botJobMethodAnno.cronExpression())
                    .intervalInSecond(intervalInSecond)
                    .concurrentCount(botJobMethodAnno.concurrentCount())
                    .autoBotJobWSParam(convertWSConfigParam(botJobMethodAnno.bowWsConfig()))
                    .uniqueAccount(botJobMethodAnno.uniqueAccount())
                    .dynamicTrigger(botJobMethodAnno.dynamicTrigger())
                    .dynamicTimeWindowMinute(botJobMethodAnno.dynamicTimeWindowMinute())
                    .syncExecute(botJobMethodAnno.syncExecute())
                    .build();

            // Step 4 设置
            setJobParam(jobName, autoBotJobParam);

            getBotApi().getBotJobService().registerJobInvoker(new JobKey(jobName, getAutoBotConfig().getBotKey()), this);

            return jobName;
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    String.format("[%s]-[%s]BotJobMethod上错误的cron表达式[%s]",
                            getBotInstance().getBotKey(), method.getName(), botJobMethodAnno.cronExpression()),
                    e
            );
        }
    }

    /**
     * 开始任务
     *
     * @param jobName jobName
     * @return BotACJobResult
     */
    public BotACJobResult startBotJob(String jobName) {
        return getBotApi().getBotJobService().startJob(
                getScriptNodeName(),
                getAutoBotConfig().getBotKey(),
                jobName,
                getJobParam(jobName),
                getInstance()
        );
    }


    /**
     * 根据账户拆分job
     *
     * @param param   param
     * @param jobName jobName
     */
    private void splitJobByAccount(AutoBotJobParam param, String jobName) {
        if (BooleanUtil.isTrue(param.getUniqueAccount())) {
            uniqueAsyncForACList(
                    (accountContext, accountContexts) -> buildNewJob(param, accountContext),
                    (accountContext, result) -> result,
                    jobName
            );
        } else {
            asyncForACList(
                    (accountContext) -> buildNewJob(param, accountContext),
                    (accountContext, result) -> result,
                    jobName
            );
        }
    }

    /**
     * 运行拆分的job
     *
     * @param jobKey       jobKey
     * @param param        param
     * @param runtimeParam runtimeParam
     */
    private void invokeAccountSplitJob(JobKey jobKey, AutoBotJobParam param, AutoBotJobRuntimeParam runtimeParam) {
        Integer acId = Integer.parseInt(jobKey.getName());

        List<AccountContext> list = getAcMap().get(acId);

        if (list.isEmpty()) {
            throw new IllegalArgumentException("不存在bot account " + acId);
        } else {
            AccountContext accountContext = list.getFirst();

            if (BooleanUtil.isTrue(param.getUniqueAccount())) {
                uniqueInvoke(accountContext, list, runtimeParam.getExtraParams(), runtimeParam.getMethod(), runtimeParam.getTarget());
            } else {
                normalInvoke(accountContext, list, runtimeParam.getExtraParams(), runtimeParam.getMethod(), runtimeParam.getTarget());
            }
        }
    }

    /**
     * 普通账户遍历处理
     *
     * @param runtimeParam runtimeParam
     * @param jobName      jobName
     * @param jobParam     jobParam
     * @return CompletableFuture<ACListOptResult>
     */
    private CompletableFuture<ACListOptResult> normalForEachAccount(
            AutoBotJobRuntimeParam runtimeParam,
            String jobName,
            AutoBotJobParam jobParam,
            AccountJobMethodInvokeHandler handler
    ) {
        Object[] extraParams = runtimeParam.getExtraParams();
        Method jobMethod = runtimeParam.getMethod();
        Object invokeObj = runtimeParam.getTarget();

        if (BooleanUtil.isTrue(jobParam.getSyncExecute())) {
            return syncForACList(
                    accountContext -> handler.invoke(accountContext, null, extraParams, jobMethod, invokeObj),
                    (accountContext, result) -> result,
                    jobName
            );
        } else {
            return asyncForACList(
                    accountContext -> handler.invoke(accountContext, null, extraParams, jobMethod, invokeObj),
                    (accountContext, result) -> result,
                    jobName
            );
        }
    }

    /**
     * 独有账户遍历处理
     *
     * @param runtimeParam runtimeParam
     * @param jobName      jobName
     * @param jobParam     jobParam
     * @return CompletableFuture<ACListOptResult>
     */
    private CompletableFuture<ACListOptResult> uniqueForEachAccount(AutoBotJobRuntimeParam runtimeParam, String jobName, AutoBotJobParam jobParam, AccountJobMethodInvokeHandler handler) {
        Object[] extraParams = runtimeParam.getExtraParams();
        Method jobMethod = runtimeParam.getMethod();
        Object invokeObj = runtimeParam.getTarget();

        if (BooleanUtil.isTrue(jobParam.getSyncExecute())) {
            return uniqueSyncForACList(
                    (accountContext, accountContexts) -> handler.invoke(accountContext, accountContexts, extraParams, jobMethod, invokeObj),
                    (accountContext, result) -> result,
                    jobName
            );
        } else {
            return uniqueAsyncForACList(
                    (accountContext, accountContexts) -> handler.invoke(accountContext, accountContexts, extraParams, jobMethod, invokeObj),
                    (accountContext, result) -> result,
                    jobName
            );
        }
    }


    private @NotNull CompletableFuture<Result> normalInvoke(AccountContext accountContext, List<AccountContext> accountContexts, Object[] extraParams, Method jobMethod, Object invokeObj) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 封装参数
                Object[] params;
                if (extraParams == null) {
                    params = new Object[]{accountContext};
                } else {
                    params = new Object[1 + extraParams.length];
                    params[0] = accountContext;
                    System.arraycopy(extraParams, 0, params, 1, extraParams.length);
                }

                // 调用执行的job method
                jobMethod.setAccessible(true);
                Object invoke = jobMethod.invoke(invokeObj, params);
                return Result.ok(invoke);
            } catch (Exception e) {
                logger.error("执行定时任务发生异常", e);
                return Result.fail("执行定时任务发生异常" + e.getCause().getMessage());
            }
        }, getExecutorService());
    }

    private @NotNull CompletableFuture<Result> uniqueInvoke(AccountContext accountContext, List<AccountContext> accountContexts, Object[] extraParams, Method jobMethod, Object invokeObj) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 封装参数
                Object[] params;
                if (extraParams == null) {
                    params = new Object[]{accountContext, accountContexts};
                } else {
                    params = new Object[2 + extraParams.length];
                    params[0] = accountContext;
                    params[1] = accountContexts;
                    System.arraycopy(extraParams, 0, params, 1, extraParams.length);
                }

                // 调用执行的job method
                jobMethod.setAccessible(true);
                Object invoke = jobMethod.invoke(invokeObj, params);
                return Result.ok(invoke);
            } catch (Exception e) {
                logger.error("执行定时任务发生异常", e);
                return Result.fail("执行定时任务发生异常" + e.getCause().getMessage());
            }
        });
    }


    /**
     * 根据account context构建新的job
     *
     * @param param          param
     * @param accountContext accountContext
     * @return CompletableFuture<Result>
     */
    private @NotNull CompletableFuture<Result> buildNewJob(
            AutoBotJobParam param,
            AccountContext accountContext
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // Step 1 计算开始时间
            long startAt = System.currentTimeMillis()
                    + (Long.max(1, random.nextInt(param.getDynamicTimeWindowMinute())) * 60 * 1000);

            // Step 2 生成参数
            String newJobName = String.valueOf(accountContext.getId());
            AutoBotJobParam jobParam = AutoBotJobParam
                    .builder()
                    .jobType(ACCOUNT_SPLIT_JOB)
                    .jobName(newJobName)
                    .intervalInSecond(param.getIntervalInSecond())
                    .autoBotJobWSParam(param.getAutoBotJobWSParam())
                    .uniqueAccount(param.getUniqueAccount())
                    .build();

            jobParam.putParam(START_AT, startAt);

            // Step 3 开始job
            getBotApi().getBotJobService().startJob(
                    getScriptNodeName(),
                    param.getJobName(),
                    newJobName,
                    jobParam,
                    getInstance(),
                    false
            );
            return Result.ok("job start at " + startAt);
        }, getExecutorService()).exceptionallyAsync(throwable -> {
            logger.error("split job by account, create new job error", throwable);
            return Result.fail("split job by account, create new job error" + throwable.getMessage());
        });
    }

    protected abstract AutoBotJobInvoker getInstance();

    /**
     * 账户列表操作结果处理
     *
     * @param future future
     */
    private void acListOptResultHandler(CompletableFuture<ACListOptResult> future) {
        future.thenAcceptAsync(acListOptResult -> {
            if (!acListOptResult.getSuccess()) {
                logger.info("botId[%s]-botName[%s]-jobName[%s] 定时任务执行失败, %s".formatted(
                        acListOptResult.getBotId(), acListOptResult.getBotName(), acListOptResult.getJobName(), acListOptResult.getErrorMsg()
                ));
            } else {
                logger.info("botId[%s]-botName[%s]-jobName[%s] 定时任务执行成功, %s".formatted(
                        acListOptResult.getBotId(), acListOptResult.getBotName(), acListOptResult.getJobName(), acListOptResult.getErrorMsg()
                ));
            }
        });
    }

    /**
     * 转换BotWSMethodConfig 为 AutoBotJobWSParam
     *
     * @param methodConfig methodConfig
     * @return AutoBotJobWSParam
     */
    private AutoBotJobWSParam convertWSConfigParam(BotWSMethodConfig methodConfig) {
        return new AutoBotJobWSParam(
                methodConfig.isRefreshWSConnection(),
                methodConfig.wsUnlimitedRetry(),
                methodConfig.nioEventLoopGroupThreads(),
                methodConfig.wsConnectCount(),
                methodConfig.reconnectLimit(),
                methodConfig.heartBeatIntervalSecond(),
                methodConfig.reconnectCountDownSecond()
        );
    }

    private interface AccountJobMethodInvokeHandler {

        CompletableFuture<Result> invoke(AccountContext accountContext, List<AccountContext> accountContexts, Object[] extraParams, Method jobMethod, Object invokeObj);

    }
}
