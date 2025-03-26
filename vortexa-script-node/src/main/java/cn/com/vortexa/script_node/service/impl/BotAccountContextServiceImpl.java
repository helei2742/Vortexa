package cn.com.vortexa.script_node.service.impl;

import cn.com.vortexa.script_node.config.AutoBotConfig;
import cn.com.vortexa.script_node.mapper.BotAccountContextMapper;
import cn.com.vortexa.script_node.service.IBotAccountContextService;
import cn.com.vortexa.common.config.SystemConfig;
import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.common.entity.BrowserEnv;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.excel.ExcelReadUtil;
import cn.com.vortexa.db_layer.plugn.table_shard.strategy.ITableShardStrategy;
import cn.com.vortexa.db_layer.service.AbstractBaseService;
import cn.com.vortexa.rpc.api.platform.IBotInstanceRPC;
import cn.com.vortexa.rpc.api.platform.IBrowserEnvRPC;
import cn.com.vortexa.control.anno.RPCReference;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@Slf4j
@Service
public class BotAccountContextServiceImpl extends AbstractBaseService<BotAccountContextMapper, AccountContext> implements IBotAccountContextService {

    public static final String BOT_ACCOUNT_CONTEXT_TABLE_PREFIX = "t_bot_account_context";

    @Autowired
    private AutoBotConfig botConfig;

    @Autowired
    private ITableShardStrategy tableShardStrategy;

    @RPCReference
    private IBotInstanceRPC botInstanceRPC;

    @RPCReference
    private IBrowserEnvRPC browserEnvRPC;

    public BotAccountContextServiceImpl() {
        super(accountContext -> {
            accountContext.setInsertDatetime(LocalDateTime.now());
            accountContext.setUpdateDatetime(LocalDateTime.now());
            accountContext.setIsValid(1);
        });
    }


    @Override
    public Result saveBotAccountContext(Integer botId, String botKey, List<Map<String, Object>> rawLines) {
        if (botId == null || StrUtil.isBlank(botKey)) {
            return Result.fail("botId或botKey不能为空");
        }

        try {
            importFromRaw(rawLines);

            return Result.ok();
        } catch (Exception e) {
            log.error("botId[{}]-botKey[{}] 报错账户信息失败", botId, botKey, e);
            return Result.fail("保存失败, " + e.getMessage());
        }
    }


    @Override
    @Transactional
    public Boolean checkAndCreateShardedTable(Integer botId, String botKey, boolean existUpdate) throws SQLException {
        if (botId == null || StrUtil.isBlank(botKey)) {
            log.error("botId/botKey为空");
            return false;
        }
        try {
            getBaseMapper().createIfTableNotExist(botId, botKey);
            return true;
        } catch (Exception e) {
            throw new SQLException("check and create sharded table[%s]-[%s] error".formatted(botId, botKey), e);
        }
    }

    @Override
    public Integer importFromExcel(String fileBotConfigPath) throws SQLException {
        String proxyFilePath = FileUtil.getConfigDirResourcePath(SystemConfig.CONFIG_DIR_APP_PATH, fileBotConfigPath);

        try {
            List<Map<String, Object>> rawLines = ExcelReadUtil.readExcelToMap(proxyFilePath);

            return importFromRaw(rawLines);
        } catch (Exception e) {
            log.error("读取twitter account 文件[{}]发生异常", proxyFilePath, e);
            return 0;
        }
    }

    @Override
    public Integer importFromRaw(List<Map<String, Object>> rawLines) throws SQLException {
        List<AccountContext> accountContexts = rawLines.stream().map(map -> AccountContext.builder()
                .botId(AutoBotConfig.BOT_ID)
                .botKey(botConfig.getBotKey())
                .accountBaseInfoId(toInteger(map.remove("account_base_info_id")))
                .twitterId(toInteger(map.remove("twitter_id")))
                .discordId(toInteger(map.remove("discord_id")))
                .proxyId(toInteger((map.remove("proxy_id"))))
                .browserEnvId(toInteger(map.remove("browser_env_id")))
                .telegramId(toInteger(map.remove("telegram_id")))
                .walletId(toInteger(map.remove("wallet_id")))
                .params(map)
                .build()
        ).toList();

        // 没设置代理的根据配置填充代理
//            tryFillProxy(accountContexts, proxyRepeat, proxyType);

        // 没设置浏览器环境的根据设置填充环境
        tryFillBrowserEnv(accountContexts);

        return insertOrUpdateBatch(accountContexts);
    }

    /**
     * 填充浏览器环境
     *
     * @param accountContexts accountContexts
     */
    private void tryFillBrowserEnv(List<AccountContext> accountContexts) {
        // Step 1 RPC 请求获取浏览器环境
        List<BrowserEnv> allBrowser = null;
        try {
            PageResult<BrowserEnv> pageResult = browserEnvRPC.conditionPageQueryRPC(1, accountContexts.size(), null);
            if (pageResult != null) {
                allBrowser = pageResult.getList();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (allBrowser == null) {
            log.warn("no browser env, please check it");
            return;
        }

        // Step 2 按次数分配
        Map<Integer, Integer> useCount = new HashMap<>();
        Map<Integer, BrowserEnv> idMapEnv = allBrowser.stream().collect(Collectors.toMap(BrowserEnv::getId, p -> {
            useCount.put(p.getId(), 0);
            return p;
        }));


        Set<AccountContext> noUseAccounts = new HashSet<>();

        accountContexts.forEach(accountContext -> {
            Integer browserEnvId = accountContext.getBrowserEnvId();

            if (browserEnvId == null || !idMapEnv.containsKey(browserEnvId)) {
                // 配置无效，给他添上
                noUseAccounts.add(accountContext);
            } else {
                useCount.put(browserEnvId, useCount.getOrDefault(browserEnvId, 0) + 1);
            }
        });

        // 填充浏览器环境
        List<Integer> ids = getLessUsedItem(useCount, noUseAccounts.size());
        ArrayList<AccountContext> list = new ArrayList<>(noUseAccounts);
        for (int i = 0; i < ids.size(); i++) {
            list.get(i).setBrowserEnvId(ids.get(i));
        }
    }

    /**
     * 获取最少使用的
     *
     * @param count 数量
     * @return List<T>
     */
    private List<Integer> getLessUsedItem(Map<Integer, Integer> useCountMap, int count) {
        if (useCountMap == null || useCountMap.isEmpty()) return Collections.emptyList();
        int batchSize = Math.min(count, useCountMap.size());

        List<Integer> res = new ArrayList<>(count);

        int needCount = count;
        while (needCount > 0) {
            int currentSize = Math.min(needCount, batchSize);

            List<Integer> batch = useCountMap.entrySet().stream()
                    .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                    .limit(currentSize)
                    .map(e -> {
                        useCountMap.compute(e.getKey(), (k, v) -> v == null ? 0 : v + 1);
                        return e.getKey();
                    }).toList();
            res.addAll(batch);

            needCount -= batch.size();
        }

        return res;
    }
}
