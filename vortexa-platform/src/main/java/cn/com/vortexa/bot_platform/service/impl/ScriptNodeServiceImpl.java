package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.bot_platform.constants.VortexaPlatFormConstants;
import cn.com.vortexa.bot_platform.script_control.BotPlatformControlServer;
import cn.com.vortexa.bot_platform.service.IBotInstanceService;
import cn.com.vortexa.bot_platform.service.IBotLaunchConfigService;
import cn.com.vortexa.bot_platform.vo.ScriptNodeDetail;
import cn.com.vortexa.bot_platform.vo.ScriptNodeVO;
import cn.com.vortexa.common.constants.ScriptNodeStatus;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.entity.BotInstance;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.ServerInstanceUtil;
import cn.com.vortexa.common.util.YamlConfigLoadUtil;
import cn.com.vortexa.control_server.dto.ConnectEntry;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.com.vortexa.bot_platform.service.IScriptNodeService;
import cn.com.vortexa.common.entity.ScriptNode;
import cn.com.vortexa.bot_platform.mapper.ScriptNodeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author com.helei
 * @since 2025-04-08
 */
@Service
public class ScriptNodeServiceImpl extends ServiceImpl<ScriptNodeMapper, ScriptNode> implements IScriptNodeService {

    @Lazy
    @Autowired
    private BotPlatformControlServer botControlServer;

    @Lazy
    @Autowired
    private IBotInstanceService botInstanceService;

    @Lazy
    @Autowired
    private IBotLaunchConfigService botLaunchConfigService;

    @Override
    public Boolean insertOrUpdate(ScriptNode scriptNode) {
        return getBaseMapper().insertOrUpdate(scriptNode) > 0;
    }

    @Override
    public List<ScriptNodeVO> queryAllScriptNode() {
        List<ScriptNode> list = list();
        Set<String> onLines = new HashSet<>(botControlServer.getConnectionService().queryOnlineInstanceKey());

        return list.stream().map(scriptNode -> {
            String key = ServerInstanceUtil.generateServiceInstanceKey(
                    scriptNode.getGroupId(), scriptNode.getServiceId(), scriptNode.getInstanceId()
            );
            List<BotInstance> scriptNodeBotInstanceList = botInstanceService.conditionQuery(
                    BotInstance.builder().scriptNodeName(scriptNode.getScriptNodeName()).build()
            );

            return ScriptNodeVO.of(
                    scriptNode,
                    onLines.contains(key),
                    scriptNodeBotInstanceList.stream().map(BotInstance::getBotKey).toList()
            );
        }).toList();
    }

    @Override
    public ScriptNode queryByScriptNodeName(String scriptNodeName) {
        ScriptNode query = new ScriptNode();
        query.setScriptNodeName(scriptNodeName);
        return getOne(new QueryWrapper<>(query));
    }

    @Override
    public ScriptNodeDetail queryScriptNodeDetail(String scriptNodeName) {
        // 查node基本信息
        ScriptNode scriptNode = queryByScriptNodeName(scriptNodeName);
        String key = ServerInstanceUtil.generateServiceInstanceKey(
                scriptNode.getGroupId(), scriptNode.getServiceId(), scriptNode.getInstanceId()
        );
        // 是否在线
        ConnectEntry connectEntry = botControlServer.getConnectionService().getServiceInstanceChannel(key);
        boolean online = connectEntry != null && connectEntry.isUsable();

        // 在线的bot查询
        Map<String, List<String>> onlineBotName2Keys = new HashMap<>();
        botControlServer.selectScriptNodeOnlineBot(key).forEach(botInstanceKey -> {
            String[] gsiArr = botInstanceKey.split(ServerInstanceUtil.SERVICE_INSTANCE_KEY_DISPATCHER);
            onlineBotName2Keys.compute(gsiArr[1], (k, v) -> {
                if (v == null) {
                    v = new ArrayList<>();
                }
                v.add(gsiArr[2]);
                return v;
            });
        });

        List<BotInstance> scriptNodeBotInstanceList = botInstanceService.conditionQuery(
                BotInstance.builder().scriptNodeName(scriptNodeName).build()
        );

        List<String> managedBotKeys = new ArrayList<>(scriptNodeBotInstanceList.size());

        // 存在的实例
        Map<String, List<String>> botNameToBotKeys = new HashMap<>();

        for (BotInstance botInstance : scriptNodeBotInstanceList) {
            botNameToBotKeys.compute(botInstance.getBotName(), (k,v)->{
                if (v == null) {
                    v = new ArrayList<>();
                }
                v.add(botInstance.getBotKey());
                return v;
            });
            managedBotKeys.add(botInstance.getBotKey());
        }

        ScriptNodeVO scriptNodeVO = ScriptNodeVO.of(
                scriptNode,
                online,
                managedBotKeys
        );
        return new ScriptNodeDetail(
                scriptNodeVO,
                botNameToBotKeys,
                onlineBotName2Keys
        );
    }

    @Override
    public String loadScriptNodeConfig(String scriptNodeName) throws IOException {
        Path dir = Paths.get(FileUtil.getScriptNodeConfigDir(), scriptNodeName);
        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
        }
        Path applicationConfigFile = dir.resolve(VortexaPlatFormConstants.SCRIPT_NODE_CONFIG_FILE);
        if (Files.exists(applicationConfigFile)) {
            return Files.readString(applicationConfigFile, StandardCharsets.UTF_8);
        } else {
            return null;
        }
    }

    @Override
    public Result startBot(String scriptNodeName, String botKey) {
        try {
            ScriptNode dbScriptNode = scriptNodeStatusCheck(scriptNodeName, botKey);
            List<BotInstance> botInstances = botInstanceService.conditionQuery(BotInstance.builder().scriptNodeName(scriptNodeName).botKey(botKey).build());
            if (CollUtil.isEmpty(botInstances)) {
                throw new IllegalArgumentException("bot instance not found");
            }
            BotInstance bot = botInstances.getFirst();
            AutoBotConfig launchConfig = botLaunchConfigService.queryOrCreateScriptNodeBotLaunchConfig(
                    scriptNodeName,
                    bot.getBotName(),
                    botKey
            );

            return botControlServer.startScriptNodeBot(
                    dbScriptNode.getGroupId(),
                    dbScriptNode.getServiceId(),
                    dbScriptNode.getInstanceId(),
                    launchConfig
            ).get();
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @Override
    public Result stopBot(String scriptNodeName, String botKey) {
        try {
            ScriptNode dbScriptNode = scriptNodeStatusCheck(scriptNodeName, botKey);

            return botControlServer.stopScriptNodeBot(
                    dbScriptNode.getGroupId(),
                    dbScriptNode.getServiceId(),
                    dbScriptNode.getInstanceId(),
                    botKey
            ).get();
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @Override
    public void saveRawScriptNodeConfig(String scriptNodeName, String nodeAppConfig) throws IOException {
        Path path = FileUtil.getScriptNodeConfig(scriptNodeName + File.separator + VortexaPlatFormConstants.SCRIPT_NODE_CONFIG_FILE);
        if (!Files.exists(path.getParent())) {
          Files.createDirectories(path.getParent());
        }
        try (FileWriter writer = new FileWriter(path.toFile())) {
            writer.write(nodeAppConfig);
        }
    }

    /**
     * 检查节点状态
     *
     * @param scriptNodeName scriptNodeName
     * @param botKey         botKey
     */
    private ScriptNode scriptNodeStatusCheck(String scriptNodeName, String botKey) {
        if (StrUtil.isBlank(scriptNodeName) || StrUtil.isBlank(botKey)) {
            throw new RuntimeException("param error");
        }
        // Step 1 查询db的scriptNode信息
        ScriptNode dbScriptNode = queryByScriptNodeName(scriptNodeName);
        if (dbScriptNode == null) {
            throw new RuntimeException("script node not exist");
        }

        // Step 2 查询scriptNode状态
        ScriptNodeStatus scriptNodeStatus = botControlServer.queryScriptNodeStatus(
                dbScriptNode.getGroupId(),
                dbScriptNode.getServiceId(),
                dbScriptNode.getInstanceId()
        );
        if (scriptNodeStatus != ScriptNodeStatus.ONLINE) {
            throw new RuntimeException("script node status[%s] is not ONLINE".formatted(scriptNodeStatus));
        }
        return dbScriptNode;
    }
}
