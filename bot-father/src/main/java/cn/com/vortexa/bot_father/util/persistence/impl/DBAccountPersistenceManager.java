package cn.com.vortexa.bot_father.util.persistence.impl;

import cn.com.vortexa.bot_father.service.BotApi;
import cn.com.vortexa.bot_father.util.persistence.AbstractPersistenceManager;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.common.entity.RewordInfo;
import cn.com.vortexa.common.util.NamedThreadFactory;
import cn.com.vortexa.common.util.propertylisten.PropertyChangeInvocation;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class DBAccountPersistenceManager extends AbstractPersistenceManager {

    private final ExecutorService executorService = Executors.newThreadPerTaskExecutor(new NamedThreadFactory("database-"));

    private final BotApi botApi;

    public DBAccountPersistenceManager(BotApi botApi) {
        this.botApi = botApi;
    }

    @Override
    public void init() {

    }

    @Override
    public void persistenceAccountContexts(List<AccountContext> accountContexts) {
        try {
            botApi.getBotAccountService().insertOrUpdateBatch(accountContexts);
        } catch (SQLException e) {
            throw new RuntimeException("persistence bot account context error", e);
        }
    }

    @Override
    public List<AccountContext> loadAccountContexts(Integer botId, String botKey) {
        // Step 1 加载 projectId 对应的账号
        Map<String, Object> query = new HashMap<>();
        query.put("botId", botId);
        query.put("botKey", botKey);

        List<AccountContext> accountContexts = null;
        try {
            accountContexts = botApi
                    .getBotAccountService()
                    .conditionQuery(query);
        } catch (SQLException e) {
            throw new RuntimeException("query bot[%s][%s] account list error".formatted(botId, botKey), e);
        }

        // Step 2 遍历账号，补充对象
        CompletableFuture<?>[] futures = accountContexts.stream()
                .map(accountContext -> CompletableFuture.runAsync(
                        () -> fillAccountInfo(accountContext), executorService))
                .toArray(CompletableFuture[]::new);

        // Step 3 等待所有任务完成
        for (int i = 0; i < futures.length; i++) {
            try {
                futures[i].get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("{} fill account context info error", i, e);
            }
        }

        // Step 4 按类型分类账号
        return accountContexts;
    }

    @Override
    protected void propertyChangeHandler(PropertyChangeInvocation invocation) {
        log.debug("object field{} changed {}->{}", invocation.getPropertyName(), invocation.getOldValue(), invocation.getNewValue());

        Object target = invocation.getTarget();
        if (target instanceof AccountContext) {
            try {
                botApi.getBotAccountService().insertOrUpdate((AccountContext) target);
            } catch (SQLException e) {
                log.error("更新Bot Account Context[{}] error", target, e);
            }
        }
    }


    /**
     * 查询填充账户信息
     *
     * @param accountContext accountContext
     */
    @Override
    public void fillAccountInfo(AccountContext accountContext) {
//        if (accountContext.getId() != 48) return;

        // Step 2.1 绑定基础账号信息
        if (accountContext.getAccountBaseInfoId() != null) {
            accountContext.setAccountBaseInfo(botApi.getAccountBaseInfoRPC().queryByIdRPC(accountContext.getAccountBaseInfoId()));
        }
        // Step 2,2 绑定推特
        if (accountContext.getTwitterId() != null) {
            accountContext.setTwitter(botApi.getTwitterAccountRPC().queryByIdRPC(accountContext.getTwitterId()));
        }
        // Step 2,3 绑定 discord
        if (accountContext.getDiscordId() != null) {
            accountContext.setDiscord(botApi.getDiscordAccountRPC().queryByIdRPC(accountContext.getDiscordId()));
        }
        // Step 2.4 绑定代理
        if (accountContext.getProxyId() != null) {
            accountContext.setProxy(botApi.getProxyInfoRPC().queryByIdRPC(accountContext.getProxyId()));
        }
        // Step 2.5 绑定浏览器环境
        if (accountContext.getBrowserEnvId() != null) {
            accountContext.setBrowserEnv(botApi.getBrowserEnvRPC().queryByIdRPC(accountContext.getBrowserEnvId()));
        }
        // Step 2.6 绑定tg
        if (accountContext.getTelegramId() != null) {
            accountContext.setTelegram(botApi.getTelegramAccountRPC().queryByIdRPC(accountContext.getBrowserEnvId()));
        }
        // Step 2.7 绑定钱包
        if (accountContext.getWalletId() != null) {
            // TODO 钱包模块
        }
        // Step 2.8 绑定奖励信息
        if (accountContext.getRewardId() != null) {
            RewordInfo rewordInfo = botApi.getRewordInfoService().queryById(accountContext.getRewardId());
            accountContext.setRewordInfo(rewordInfo);
        }
    }
}
