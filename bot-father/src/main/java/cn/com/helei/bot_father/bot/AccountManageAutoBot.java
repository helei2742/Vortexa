package cn.com.helei.bot_father.bot;

import cn.com.helei.bot_father.util.persistence.AccountPersistenceManager;
import cn.com.helei.bot_father.util.persistence.impl.DBAccountPersistenceManager;
import cn.com.helei.common.dto.ACListOptResult;
import cn.com.helei.common.dto.BotACJobResult;
import cn.com.helei.common.dto.Result;
import cn.com.helei.common.dto.job.AutoBotJobParam;
import cn.com.helei.common.entity.AccountContext;
import cn.com.helei.common.exception.BotInitException;
import cn.hutool.core.util.BooleanUtil;
import com.alibaba.fastjson.JSONArray;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.com.helei.common.entity.BotInfo.ACCOUNT_PARAMS_KEY;


public abstract class AccountManageAutoBot extends AbstractAutoBot {

    /**
     * job并发控制信号量， jobName -> semaphore
     */
    private final ConcurrentMap<String, Semaphore> jobCCSemaphoreMap = new ConcurrentHashMap<>();


    @Getter
    private final List<AccountContext> accountContexts = new ArrayList<>();


    @Getter
    private final Map<Integer, List<AccountContext>> acMap = new HashMap<>();

    @Getter
    private final List<AccountContext> uniqueACList = new ArrayList<>();

    /**
     * 持久化管理器
     */
    @Getter
    private AccountPersistenceManager persistenceManager;

    @Override
    protected void doInit() throws BotInitException {
        // Step 1 初始化保存的线程
        this.persistenceManager = new DBAccountPersistenceManager(getBotApi());

        this.persistenceManager.init();

        // Step 2 初始化账户
        this.initAccounts();

        acMap.putAll(
                getAccountContexts()
                        .stream()
                        .collect(Collectors.groupingBy(ac ->
                                ac.getAccountBaseInfoId() == null ? -1 : ac.getAccountBaseInfoId())
                        )
        );

        uniqueACList.addAll(acMap.values().stream().map(List::getFirst).toList());
    }

    /**
     * 注册账户
     *
     * @return CompletableFuture<Result>
     */
    public abstract CompletableFuture<ACListOptResult> registerAccount();

    /**
     * 登录并获取token
     *
     * @return CompletableFuture<Result>
     */
    public abstract CompletableFuture<ACListOptResult> loginAndTakeTokenAccount();


    /**
     * 更新账户奖励信息
     *
     * @return CompletableFuture<Result>
     */
    public abstract CompletableFuture<ACListOptResult> updateAccountRewordInfo();


    /**
     * 获取jb name列表
     *
     * @return List<String>
     */
    public abstract Set<String> botJobNameList();

    /**
     * 账号被加载后调用
     *
     * @param accountContexts accountContexts
     */
    protected void accountsLoadedHandler(List<AccountContext> accountContexts) {
    }


    public CompletableFuture<ACListOptResult> uniqueAsyncForACList(
            BiFunction<AccountContext, List<AccountContext>, CompletableFuture<Result>> buildResultFuture,
            BiFunction<AccountContext, BotACJobResult, BotACJobResult> resultHandler,
            String jobName
    ) {

        return asyncForACList(
                new ArrayList<>(uniqueACList),
                accountContext -> buildResultFuture.apply(accountContext, acMap.get(accountContext.getAccountBaseInfoId())),
                resultHandler,
                jobName
        );
    }

    public CompletableFuture<ACListOptResult> uniqueSyncForACList(
            BiFunction<AccountContext, List<AccountContext>, CompletableFuture<Result>> buildResultFuture,
            BiFunction<AccountContext, BotACJobResult, BotACJobResult> resultHandler,
            String jobName
    ) {

        return syncForACList(
                new ArrayList<>(uniqueACList),
                accountContext -> buildResultFuture.apply(accountContext, acMap.get(accountContext.getAccountBaseInfoId())),
                resultHandler,
                jobName
        );
    }

    /**
     * 遍历账户
     *
     * @param buildResultFuture buildResultFuture   具体执行的方法
     * @param resultHandler     resultHandler   处理结果的方法
     * @return CompletableFuture<ACListOptResult>
     */
    public CompletableFuture<ACListOptResult> syncForACList(
            Function<AccountContext, CompletableFuture<Result>> buildResultFuture,
            BiFunction<AccountContext, BotACJobResult, BotACJobResult> resultHandler,
            String jobName
    ) {
        return syncForACList(getAccountContexts(), buildResultFuture, resultHandler, jobName);
    }

    /**
     * 异步遍历账户
     *
     * @param buildResultFuture buildResultFuture   具体执行的方法
     * @param resultHandler     resultHandler   处理结果的方法
     * @return CompletableFuture<ACListOptResult>
     */
    public CompletableFuture<ACListOptResult> asyncForACList(
            Function<AccountContext, CompletableFuture<Result>> buildResultFuture,
            BiFunction<AccountContext, BotACJobResult, BotACJobResult> resultHandler,
            String jobName
    ) {
        return asyncForACList(getAccountContexts(), buildResultFuture, resultHandler, jobName);
    }

    /**
     * 同步遍历账户
     *
     * @param buildResultFuture buildResultFuture   具体执行的方法
     * @param resultHandler     resultHandler   处理结果的方法
     * @return CompletableFuture<ACListOptResult>
     */
    public CompletableFuture<ACListOptResult> syncForACList(
            List<AccountContext> accountContexts,
            Function<AccountContext, CompletableFuture<Result>> buildResultFuture,
            BiFunction<AccountContext, BotACJobResult, BotACJobResult> resultHandler,
            String jobName
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<BotACJobResult> results = new ArrayList<>();
            int successCount = 0;

            for (AccountContext accountContext : new HashSet<>(accountContexts)) {
                if (checkAccountContainsParams(accountContext)) {
                    BotACJobResult botACJobResult = new BotACJobResult(
                            getBotInfo().getId(),
                            getBotInfo().getName(),
                            jobName,
                            accountContext.getId()
                    );

                    CompletableFuture<Result> future = buildResultFuture.apply(accountContext);

                    try {
                        Result result = future.get();
                        if (result.getSuccess()) successCount++;
                        botACJobResult = resultHandler.apply(accountContext, botACJobResult.setResult(result));
                        results.add(botACJobResult);
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            return ACListOptResult.builder()
                    .botId(getBotInfo().getId())
                    .botName(getBotInfo().getName())
                    .jobName(jobName)
                    .successCount(successCount)
                    .success(true)
                    .results(results)
                    .build();
        }, getExecutorService());
    }


    /**
     * 异步遍历账户
     *
     * @param buildResultFuture buildResultFuture   具体执行的方法
     * @param resultHandler     resultHandler   处理结果的方法
     * @return CompletableFuture<ACListOptResult>
     */
    public CompletableFuture<ACListOptResult> asyncForACList(
            List<AccountContext> accountContexts,
            Function<AccountContext, CompletableFuture<Result>> buildResultFuture,
            BiFunction<AccountContext, BotACJobResult, BotACJobResult> resultHandler,
            String jobName
    ) {
        // Step 1 遍历账户，获取执行结果
        List<CompletableFuture<BotACJobResult>> futures = accountContexts.stream()
                .filter(this::checkAccountContainsParams)
                .map(accountContext -> {
                    try {
                        // 获取信号量
                        getCcSemaphore(jobName).acquire();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    BotACJobResult botACJobResult;
                    CompletableFuture<Result> future;

                    try {
                        botACJobResult = new BotACJobResult(
                                getBotInfo().getId(),
                                getBotInfo().getName(),
                                jobName,
                                accountContext.getId()
                        );

                        future = buildResultFuture.apply(accountContext);

                    } catch (Exception e) {
                        getCcSemaphore(jobName).release();
                        throw new RuntimeException(e);
                    }

                    return future.thenApplyAsync(botACJobResult::setResult, getExecutorService())
                            .whenComplete((result, throwable) -> {
                                // 释放信号量
                                getCcSemaphore(jobName).release();
                            });
                }).toList();

        // Step 2 等待执行完成，转换执行结果
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApplyAsync(unused -> {
                    List<BotACJobResult> results = new ArrayList<>();

                    int success = 0;
                    for (int i = 0; i < futures.size(); i++) {
                        CompletableFuture<BotACJobResult> future = futures.get(i);
                        AccountContext accountContext = accountContexts.get(i);

                        try {
                            BotACJobResult result = future.get();
                            BotACJobResult botACJobResult = resultHandler.apply(accountContext, result);

                            if (BooleanUtil.isTrue(botACJobResult.getSuccess())) success++;

                            results.add(botACJobResult);
                        } catch (InterruptedException | ExecutionException e) {
                            String errorMsg = String.format("[%s] %s 获取异步结果发生错误",
                                    accountContext.getId(), accountContext.getSimpleInfo());

                            logger.error(errorMsg, e);

                            results.add(
                                    new BotACJobResult(
                                            getBotInfo().getId(),
                                            getBotInfo().getName(),
                                            jobName,
                                            accountContext.getId(),
                                            false,
                                            errorMsg,
                                            null
                                    )
                            );
                        }
                    }

                    return ACListOptResult.builder()
                            .botId(getBotInfo().getId())
                            .botName(getBotInfo().getName())
                            .jobName(jobName)
                            .successCount(success)
                            .success(true)
                            .results(results)
                            .build();
                });
    }

    /**
     * 检查账户是否含有指定参数
     *
     * @param accountContext accountContext
     * @return boolean
     */
    private boolean checkAccountContainsParams(AccountContext accountContext) {
        // 过滤掉没有账户需要参数的
        Object o = getBotInfo().getParams().get(ACCOUNT_PARAMS_KEY);
        // 使用的json序列化进db，反序列化得到的是JsonArray
        if (o instanceof JSONArray jsonArray) {
            for (Object obj : jsonArray) {
                String key = (String) obj;
                if (accountContext.getParam(key) == null) {
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * 初始化账号方法
     */
    private void initAccounts() throws BotInitException {
        Integer botId = getBotInfo().getId();

        try {
            logger.info("开始加载账户数据");
            // Step 1 获取持久化的
            List<AccountContext> accountContexts = persistenceManager
                    .loadAccountContexts(botId, getAutoBotConfig().getBotKey());

            // Step 2 没有保存的数据
            if (accountContexts == null || accountContexts.isEmpty()) {
                logger.warn("没有账户数据");
            } else {
                logger.info("使用历史账户数据, 共:" + accountContexts.size());

                // Step 3 加载到bot (字段修改监听)
                registerAccountsInBot(accountContexts);

                accountsLoadedHandler(accountContexts);

                this.accountContexts.addAll(accountContexts);
            }
        } catch (Exception e) {
            throw new BotInitException("初始化账户发生错误", e);
        }
    }


    /**
     * 将账户加载到bot， 会注册监听，当属性发生改变时自动刷入磁盘
     *
     * @param accountContexts accountContexts
     */
    private void registerAccountsInBot(List<AccountContext> accountContexts) {
        persistenceManager.registerPersistenceListener(accountContexts);
    }


    /**
     * 获取并发控制的信号量
     *
     * @param jobName jobName
     * @return Semaphore
     */
    private Semaphore getCcSemaphore(String jobName) {
        return jobCCSemaphoreMap.computeIfAbsent(jobName, key -> {
            AutoBotJobParam autoBotJobParam = getJobParam(key);
            if (autoBotJobParam == null) {
                return new Semaphore(getRequestConcurrentCount());
            }
            return new Semaphore(autoBotJobParam.getConcurrentCount());
        });
    }

}
