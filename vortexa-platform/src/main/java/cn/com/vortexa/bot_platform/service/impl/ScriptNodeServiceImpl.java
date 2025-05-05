package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.bot_platform.constants.VortexaPlatFormConstants;
import cn.com.vortexa.bot_platform.script_control.BotPlatformControlServer;
import cn.com.vortexa.bot_platform.vo.ScriptNodeVO;
import cn.com.vortexa.common.constants.ScriptNodeStatus;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.control.util.ControlServerUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.com.vortexa.bot_platform.service.IScriptNodeService;
import cn.com.vortexa.common.entity.ScriptNode;
import cn.com.vortexa.bot_platform.mapper.ScriptNodeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

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

    @Override
    public Boolean insertOrUpdate(ScriptNode scriptNode) {
        return getBaseMapper().insertOrUpdate(scriptNode) > 0;
    }

    @Override
    public List<ScriptNodeVO> queryAllScriptNode() {
        List<ScriptNode> list = list();
        Set<String> onLines = new HashSet<>(botControlServer.getConnectionService().queryOnlineInstanceKey());

        return list.stream().map(scriptNode -> {
            String key = ControlServerUtil.generateServiceInstanceKey(
                    scriptNode.getGroupId(), scriptNode.getServiceId(), scriptNode.getInstanceId()
            );
            return ScriptNodeVO.of(
                    scriptNode,
                    onLines.contains(key),
                    new ArrayList<>(scriptNode.getBotConfigMap().keySet())
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
    public String loadScriptNodeBotLaunchConfig(String scriptNodeName, String botKey) throws IOException {
        Path dir = Paths.get(FileUtil.getScriptNodeConfigDir(), scriptNodeName, botKey);
        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
        }
        Path botLaunchConfigFile = dir.resolve(VortexaPlatFormConstants.SCRIPT_NODE_BOT_CONFIG_FILE);
        if (Files.exists(botLaunchConfigFile)) {
            return Files.readString(botLaunchConfigFile, StandardCharsets.UTF_8);
        } else {
            return null;
        }
    }

    @Override
    public void updateScriptNodeBotLaunchConfig(String scriptNodeName, String botKey, String botLaunchConfig) throws IOException {
        Path dir = Paths.get(FileUtil.getScriptNodeConfigDir(), scriptNodeName, botKey);
        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
        }
        Path botLaunchConfigFile = dir.resolve(VortexaPlatFormConstants.SCRIPT_NODE_BOT_CONFIG_FILE);
        Files.writeString(botLaunchConfigFile, botLaunchConfig, StandardCharsets.UTF_8);
    }

    @Override
    public Result startBot(String scriptNodeName, String botKey) {
        try {
            ScriptNode dbScriptNode = scriptNodeStatusCheck(scriptNodeName, botKey);

            return botControlServer.startScriptNodeBot(
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
