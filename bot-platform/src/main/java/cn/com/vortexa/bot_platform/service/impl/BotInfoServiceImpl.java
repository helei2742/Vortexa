package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.db_layer.service.AbstractBaseService;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.BotInfo;
import cn.com.vortexa.db_layer.mapper.BotInfoMapper;
import cn.com.vortexa.rpc.IBotInfoRPC;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-07
 */
@Slf4j
@DubboService
public class BotInfoServiceImpl extends AbstractBaseService<BotInfoMapper, BotInfo> implements IBotInfoRPC {


    public BotInfoServiceImpl() {
        super(botInfo -> {
            botInfo.setInsertDatetime(LocalDateTime.now());
            botInfo.setUpdateDatetime(LocalDateTime.now());
            botInfo.setIsValid(1);
        });
    }


    @Override
    public Result bindBotAccountBaseInfo(Integer botId, String botKey, List<Integer> bindAccountBaseInfoList) {

        // Step 1 参数校验
        if (botId == null || StrUtil.isBlank(botKey)) {
            log.error("Bot[{}]-[{}]绑定账户时参数botId/botKey为空", botId, botKey);
            return Result.fail("参数botId/botKey不能为空");
        }

        BotInfo dbBotInfo = query().eq("id", botId).one();
        if (dbBotInfo == null) {
            log.error("Bot[{}]-[{}] 不存在该bot", botId, botKey);
            return Result.fail("不存在该bot");
        }

        if (bindAccountBaseInfoList == null) bindAccountBaseInfoList = List.of();

        return Result.ok(bindAccountBaseInfoList);
//        // Step 2 查看或创建对应的表
//        try {
//            botAccountContextService.checkAndCreateShardedTable(botId, botKey, false);
//        } catch (SQLException e) {
//            log.error("检查创建[{}]-[{}]对应表失败", botId, botKey);
//            return Result.fail("检查创建表失败, " + e.getMessage());
//        }

//        // Step 3 插入或更新数据
//        List<BrowserEnv> browserEnvs = browserEnvService.getUselessBrowserEnv(bindAccountBaseInfoList.size());
//        AtomicInteger idx = new AtomicInteger();
//
//        List<AccountContext> accountContexts = bindAccountBaseInfoList.stream().map(abId -> {
//            AccountContext accountContext = new AccountContext();
//            accountContext.setBotId(botId);
//            accountContext.setBotKey(botKey);
//            accountContext.setAccountBaseInfoId(abId);
//            accountContext.setBrowserEnvId(browserEnvs.get(idx.getAndIncrement()).getId());
//
//            return accountContext;
//        }).toList();
//
//        Integer i = null;
//        try {
//            i = botAccountContextService.insertOrUpdateBatch(accountContexts);
//
//            log.info("保存Bot[{}]-[{}]账户成功. [{}/{}]", botId, botKey, i, accountContexts.size());
//            return Result.ok("保存Bot账户成功");
//        } catch (SQLException e) {
//            log.error("保存Bot[{}]-[{}]账户失败", botId, botKey, e);
//            return Result.fail("保存Bot账户失败, " + e.getCause().getMessage());
//        }
    }
}
