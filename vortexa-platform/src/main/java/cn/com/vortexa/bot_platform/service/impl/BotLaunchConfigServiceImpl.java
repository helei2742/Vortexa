package cn.com.vortexa.bot_platform.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.com.vortexa.bot_platform.entity.BotLaunchConfig;
import cn.com.vortexa.bot_platform.mapper.BotLaunchConfigMapper;
import cn.com.vortexa.bot_platform.service.IBotInfoService;
import cn.com.vortexa.bot_platform.service.IBotInstanceService;
import cn.com.vortexa.bot_platform.service.IBotLaunchConfigService;
import cn.com.vortexa.bot_platform.service.IScriptNodeService;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.config.AutoBotAccountConfig;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.entity.BotInfo;
import cn.com.vortexa.common.entity.BotInstance;
import cn.com.vortexa.common.entity.ScriptNode;
import cn.com.vortexa.rpc.api.platform.IBotLaunchConfigRPC;
import cn.hutool.core.util.StrUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public Result create(BotLaunchConfig botLaunchConfig) {
        String scriptNodeName = botLaunchConfig.getScriptNodeName();
        String botName = botLaunchConfig.getBotName();
        String botKey = botLaunchConfig.getBotKey();
        if (StrUtil.isBlank(scriptNodeName) || StrUtil.isBlank(botName) || StrUtil.isBlank(botKey)) {
            throw new IllegalArgumentException("param can't be empty");
        }
        if (botInstanceService.exist(BotInstance.builder().scriptNodeName(scriptNodeName).botKey(botKey).build())) {
            throw new RuntimeException("bot instance already exist");
        }
        ScriptNode scriptNode = scriptNodeService.queryByScriptNodeName(scriptNodeName);
        if (scriptNode == null) {
            throw new RuntimeException("script node not exist");
        }
        if (!botInfoService.exist((BotInfo.builder().name(botName).build()))) {
            throw new RuntimeException("bot not exist");
        }

        if (baseMapper.insert(botLaunchConfig) > 0) {
            return Result.ok();
        }
        return Result.fail("create bot launch config fail");
    }

    @Override
    public AutoBotConfig queryScriptNodeBotLaunchConfig(String scriptNodeName, String botKey) {
        BotLaunchConfig botLaunchConfig = getOne(
                new QueryWrapper<>(BotLaunchConfig.builder().scriptNodeName(scriptNodeName).build()));
        return conventerBotLaunchConfig2AutoBotConfig(botLaunchConfig);
    }

    @Override
    public List<AutoBotConfig> queryScriptNodeAllBotLaunchConfig(String scriptNodeName) {
        List<BotLaunchConfig> launchConfigs = list(
                new QueryWrapper<>(BotLaunchConfig.builder().scriptNodeName(scriptNodeName).build()));

        return launchConfigs.stream().map(BotLaunchConfigServiceImpl::conventerBotLaunchConfig2AutoBotConfig).toList();
    }

    private static @NotNull AutoBotConfig conventerBotLaunchConfig2AutoBotConfig(BotLaunchConfig botLaunchConfig) {
        AutoBotConfig botConfig = new AutoBotConfig();
        botConfig.setBotName(botLaunchConfig.getBotName());
        botConfig.setBotKey(botLaunchConfig.getBotKey());
        botConfig.setCustomConfig(botLaunchConfig.getCustomConfig());
        return botConfig;
    }
}
