package cn.com.vortexa.bot_platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.com.vortexa.bot_platform.entity.BotLaunchConfig;
import cn.com.vortexa.bot_platform.mapper.BotLaunchConfigMapper;
import cn.com.vortexa.bot_platform.service.IBotInfoService;
import cn.com.vortexa.bot_platform.service.IBotInstanceService;
import cn.com.vortexa.bot_platform.service.IBotLaunchConfigService;
import cn.com.vortexa.bot_platform.service.IScriptNodeService;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.entity.BotInfo;
import cn.com.vortexa.common.entity.BotInstance;
import cn.com.vortexa.common.entity.ScriptNode;
import cn.com.vortexa.rpc.api.platform.IBotLaunchConfigRPC;
import cn.hutool.core.util.StrUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author helei
 * @since 2025/5/8 15:06
 */
@Service
public class BotLaunchConfigServiceImpl extends ServiceImpl<BotLaunchConfigMapper, BotLaunchConfig>
        implements IBotLaunchConfigService, IBotLaunchConfigRPC {

    @Autowired
    private IScriptNodeService scriptNodeService;

    @Autowired
    private IBotInfoService botInfoService;

    @Autowired
    private IBotInstanceService botInstanceService;

    @Override
    @Transactional
    public Result create(BotLaunchConfig botLaunchConfig) throws SQLException {
        String scriptNodeName = botLaunchConfig.getScriptNodeName();
        String botName = botLaunchConfig.getBotName();
        String botKey = botLaunchConfig.getBotKey();
        if (StrUtil.isBlank(scriptNodeName) || StrUtil.isBlank(botName) || StrUtil.isBlank(botKey)) {
            throw new IllegalArgumentException("param can't be empty");
        }
        ScriptNode scriptNode = scriptNodeService.queryByScriptNodeName(scriptNodeName);
        if (scriptNode == null) {
            throw new RuntimeException("script node not exist");
        }
        BotInfo botInfo = botInfoService.queryByName(botName);
        if (botInfo == null) {
            throw new RuntimeException("bot not exist");
        }
        if (baseMapper.exists(new QueryWrapper<>(BotLaunchConfig.builder()
                .scriptNodeName(scriptNodeName).botKey(botKey).build()))
        ) {
            throw new RuntimeException("bot launch config already exist");
        }

        if (baseMapper.insert(botLaunchConfig) > 0) {
            BotInstance existBotInstance = botInstanceService.getOne(scriptNodeName, botKey);
            BotInstance.BotInstanceBuilder builder = BotInstance
                    .builder()
                    .botId(botInfo.getId())
                    .botName(botName)
                    .botKey(botKey)
                    .scriptNodeName(scriptNodeName)
                    .versionCode(botInfo.getVersionCode());
            if (existBotInstance != null) {
                builder.jobParams(existBotInstance.getJobParams() == null ? botInfo.getJobParams() : null);
            } else {
                builder.jobParams(botInfo.getJobParams());
            }
            botInstanceService.insertOrUpdate(builder.build());
            return Result.ok();
        }
        return Result.fail("create bot launch config fail");
    }

    @Override
    public AutoBotConfig queryScriptNodeBotLaunchConfig(String scriptNodeName, String botKey) {
        BotLaunchConfig botLaunchConfig = getOne(
                new QueryWrapper<>(BotLaunchConfig.builder().scriptNodeName(scriptNodeName).botKey(botKey).build()));
        if (botLaunchConfig == null) return null;
        return conventerBotLaunchConfig2AutoBotConfig(botLaunchConfig);
    }

    @Override
    public AutoBotConfig queryOrCreateScriptNodeBotLaunchConfig(String scriptNodeName, String botName, String botKey) {
        BotLaunchConfig launchConfig = BotLaunchConfig.builder().scriptNodeName(scriptNodeName).botKey(botKey).build();
        BotLaunchConfig dbLaunchConfig = getOne(new QueryWrapper<>(launchConfig));
        if (dbLaunchConfig == null) {
            launchConfig.setCustomConfig(new HashMap<>());
            launchConfig.setBotName(botName);
            insertOrUpdate(launchConfig);
            return conventerBotLaunchConfig2AutoBotConfig(launchConfig);
        } else {
            return conventerBotLaunchConfig2AutoBotConfig(dbLaunchConfig);
        }
    }

    @Override
    public List<AutoBotConfig> queryScriptNodeAllBotLaunchConfig(String scriptNodeName) {
        List<BotLaunchConfig> launchConfigs = list(
                new QueryWrapper<>(BotLaunchConfig.builder().scriptNodeName(scriptNodeName).build()));

        return launchConfigs.stream().map(BotLaunchConfigServiceImpl::conventerBotLaunchConfig2AutoBotConfig).toList();
    }

    @Override
    public Result saveBotLaunchConfig(
            String scriptNodeName,
            String botName,
            String botKey,
            Map<String, Object> botLaunchConfig
    ) {
        if (StrUtil.isBlank(scriptNodeName) || StrUtil.isBlank(botKey) || StrUtil.isBlank(botName) || botLaunchConfig == null) {
            throw new IllegalArgumentException("param can't be empty");
        }
        ScriptNode snQuery = new ScriptNode();
        snQuery.setScriptNodeName(scriptNodeName);
        if (!scriptNodeService.exists(new QueryWrapper<>(snQuery))) {
            throw new RuntimeException("script node not exist");
        }
        if (!botInstanceService.exist(BotInstance.builder().build())) {
            throw new RuntimeException("bot instance not exist");
        }

        if (insertOrUpdate(BotLaunchConfig.builder()
                .scriptNodeName(scriptNodeName).botKey(botKey).botName(botName).customConfig(botLaunchConfig)
                .build()) > 0
        ) {
            return Result.ok();
        }
        return Result.fail("save bot launch config fail");
    }

    @Override
    public int insertOrUpdate(BotLaunchConfig botLaunchConfig) {
        return getBaseMapper().insertOrUpdate(botLaunchConfig);
    }

    private static @NotNull AutoBotConfig conventerBotLaunchConfig2AutoBotConfig(BotLaunchConfig botLaunchConfig) {
        AutoBotConfig botConfig = new AutoBotConfig();
        botConfig.setBotName(botLaunchConfig.getBotName());
        botConfig.setBotKey(botLaunchConfig.getBotKey());
        botConfig.setCustomConfig(botLaunchConfig.getCustomConfig());
        return botConfig;
    }
}
