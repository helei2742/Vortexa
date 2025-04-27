package cn.com.vortexa.script_node.util.persistence.impl;

import cn.com.vortexa.common.entity.*;
import cn.com.vortexa.script_node.service.BotApi;
import cn.com.vortexa.script_node.util.persistence.AbstractPersistenceManager;
import cn.com.vortexa.common.util.propertylisten.PropertyChangeInvocation;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
public class DBAccountPersistenceManager extends AbstractPersistenceManager {

    private final ExecutorService executorService;

    private final BotApi botApi;

    public DBAccountPersistenceManager(BotApi botApi, ExecutorService executorService) {
        this.botApi = botApi;
        this.executorService = executorService;
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
        try {
            fillAccountInfos(accountContexts);
        } catch (Exception e) {
            throw new RuntimeException("fill account info error", e);
        }

        // Step 4
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
        } else if (target instanceof RewordInfo) {

        }
    }

    @Override
    public void fillAccountInfos(List<AccountContext> accountContexts) throws ExecutionException, InterruptedException {
        Set<Integer> baseAccountIds = new HashSet<>();
        Set<Integer> twitterIds = new HashSet<>();
        Set<Integer> discordIds = new HashSet<>();
        Set<Integer> proxyIds = new HashSet<>();
        Set<Integer> browserIds = new HashSet<>();
        Set<Integer> telegramIds = new HashSet<>();
        Set<Integer> rewordInfoIds = new HashSet<>();
        Set<Integer> walletIds = new HashSet<>();

        accountContexts.forEach(accountContext -> {
            baseAccountIds.add(accountContext.getAccountBaseInfoId());
            twitterIds.add(accountContext.getTwitterId());
            discordIds.add(accountContext.getDiscordId());
            proxyIds.add(accountContext.getProxyId());
            browserIds.add(accountContext.getBrowserEnvId());
            telegramIds.add(accountContext.getTelegramId());
            walletIds.add(accountContext.getWalletId());
            rewordInfoIds.add(accountContext.getId());
        });

        CompletableFuture<Map<Integer, AccountBaseInfo>> accountBaseInfoMapFuture = CompletableFuture.supplyAsync(() -> {
            baseAccountIds.remove(null);
            return botApi.getAccountBaseInfoRPC()
                    .batchQueryByIdsRPC(new ArrayList<>(baseAccountIds))
                    .stream()
                    .collect(Collectors.toMap(AccountBaseInfo::getId, accountBaseInfo -> accountBaseInfo));
        }, executorService);

        CompletableFuture<Map<Integer, TwitterAccount>> twitterAccountMapFuture = CompletableFuture.supplyAsync(() -> {
            twitterIds.remove(null);
            return botApi.getTwitterAccountRPC()
                    .batchQueryByIdsRPC(new ArrayList<>(twitterIds))
                    .stream()
                    .collect(Collectors.toMap(TwitterAccount::getId, twitterAccount -> twitterAccount));
        }, executorService);

        CompletableFuture<Map<Integer, DiscordAccount>> discordAccountMapFuture = CompletableFuture.supplyAsync(() -> {
            discordIds.remove(null);
            return botApi.getDiscordAccountRPC()
                    .batchQueryByIdsRPC(new ArrayList<>(discordIds))
                    .stream()
                    .collect(Collectors.toMap(DiscordAccount::getId, discordAccount -> discordAccount));
        }, executorService);

        CompletableFuture<Map<Integer, ProxyInfo>> proxyInfoMapFuture = CompletableFuture.supplyAsync(() -> {
            proxyIds.remove(null);
            return botApi.getProxyInfoRPC()
                    .batchQueryByIdsRPC(new ArrayList<>(proxyIds))
                    .stream()
                    .collect(Collectors.toMap(ProxyInfo::getId, proxyInfo -> proxyInfo));
        }, executorService);

        CompletableFuture<Map<Integer, BrowserEnv>> browserEnvMapFuture = CompletableFuture.supplyAsync(() -> {
            browserIds.remove(null);
            return botApi.getBrowserEnvRPC()
                    .batchQueryByIdsRPC(new ArrayList<>(browserIds))
                    .stream()
                    .collect(Collectors.toMap(BrowserEnv::getId, browserEnv -> browserEnv));
        }, executorService);

        CompletableFuture<Map<Integer, TelegramAccount>> telegramAccountMapFuture = CompletableFuture.supplyAsync(() -> {
            telegramIds.remove(null);
            return botApi.getTelegramAccountRPC()
                    .batchQueryByIdsRPC(new ArrayList<>(telegramIds))
                    .stream()
                    .collect(Collectors.toMap(TelegramAccount::getId, account -> account));
        }, executorService);

        CompletableFuture<Map<Integer, Web3Wallet>> walletMapFuture = CompletableFuture.supplyAsync(() -> {
            walletIds.remove(null);
            return botApi.getWeb3WalletRPC()
                    .batchQueryByIdsRPC(new ArrayList<>(walletIds))
                    .stream()
                    .collect(Collectors.toMap(Web3Wallet::getId, account -> account));
        }, executorService);

        Map<Integer, AccountBaseInfo> accountBaseInfoMap = accountBaseInfoMapFuture.get();
        Map<Integer, TwitterAccount> twitterAccountMap = twitterAccountMapFuture.get();
        Map<Integer, DiscordAccount> discordAccountMap = discordAccountMapFuture.get();
        Map<Integer, ProxyInfo> proxyInfoMap = proxyInfoMapFuture.get();
        Map<Integer, BrowserEnv> browserEnvMap = browserEnvMapFuture.get();
        Map<Integer, TelegramAccount> telegramAccountMap = telegramAccountMapFuture.get();
        Map<Integer, Web3Wallet> walletMap = walletMapFuture.get();

        accountContexts.forEach(accountContext -> {
            accountContext.setAccountBaseInfo(accountBaseInfoMap.get(accountContext.getAccountBaseInfoId()));
            accountContext.setTwitter(twitterAccountMap.get(accountContext.getTwitterId()));
            accountContext.setDiscord(discordAccountMap.get(accountContext.getDiscordId()));
            accountContext.setProxy(proxyInfoMap.get(accountContext.getProxyId()));
            accountContext.setBrowserEnv(browserEnvMap.get(accountContext.getBrowserEnvId()));
            accountContext.setTelegram(telegramAccountMap.get(accountContext.getTelegramId()));
            accountContext.setWallet(walletMap.get(accountContext.getWalletId()));
        });
    }
}
