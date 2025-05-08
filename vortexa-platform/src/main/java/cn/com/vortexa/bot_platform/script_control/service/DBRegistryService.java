package cn.com.vortexa.bot_platform.script_control.service;

import cn.com.vortexa.bot_platform.constants.VortexaPlatFormConstants;
import cn.com.vortexa.bot_platform.service.IScriptNodeService;
import cn.com.vortexa.common.dto.control.RegisteredScriptNode;
import cn.com.vortexa.common.dto.control.ServiceInstance;
import cn.com.vortexa.common.entity.ScriptNode;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.YamlConfigLoadUtil;
import cn.com.vortexa.control.constant.RegistryState;
import cn.com.vortexa.control_server.service.IRegistryService;
import cn.com.vortexa.common.util.ServerInstanceUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * 数据库注册服务
 *
 * @author com.helei
 * @since 2025/4/8 17:37
 */
@Slf4j
public class DBRegistryService implements IRegistryService {

    private final IScriptNodeService scriptNodeService;
    private final LinkedBlockingQueue<RegisteredScriptNode> updatedCache = new LinkedBlockingQueue<>();
    private boolean running = true;

    public DBRegistryService(
            IScriptNodeService scriptNodeService,
            ExecutorService executorService
    ) {
        this.scriptNodeService = scriptNodeService;

        executorService.execute(() -> {
            while (running) {
                try {
                    saveRegistryInfo();
                } catch (InterruptedException e) {
                    log.warn("save registry info task interrupted");
                    running = false;
                } catch (Exception e) {
                    log.error("save registry error", e);
                }
            }
        });
    }

    @Override
    public RegistryState registryService(ServiceInstance serviceInstance) {
        if (serviceInstance == null) {
            return RegistryState.PARAM_ERROR;
        }
        if (serviceInstance instanceof ScriptNode scriptNode) {
            if (!scriptNode.usable() || StrUtil.isBlank(scriptNode.getScriptNodeName())) {
                return RegistryState.PARAM_ERROR;
            }
            try {
                String scriptNodeName = scriptNode.getScriptNodeName();

                // 保存script node 对应的application.yaml文件
                trySaveRawScriptNodeApplicationConfig(scriptNodeName, scriptNode.getNodeAppConfig());
                scriptNodeService.insertOrUpdate(scriptNode);
                return RegistryState.OK;
            } catch (Exception e) {
                log.error("update registry error", e);
                return RegistryState.UNKNOWN_ERROR;
            }
        }

        return RegistryState.STORE_ERROR;
    }

    @Override
    public Boolean saveRegistryInfo() throws InterruptedException {
        RegisteredScriptNode take = updatedCache.take();
        log.debug("start save registry info - [{}]", take);
        return scriptNodeService.insertOrUpdate(take.getScriptNode());
    }

    @Override
    public List<RegisteredScriptNode> queryServiceInstance(ServiceInstance query) {
        List<ScriptNode> list = null;
        if (query == null) {
            list = scriptNodeService.list();
        } else if (query instanceof ScriptNode scriptNode) {
            list = scriptNodeService.list(new QueryWrapper<>(scriptNode));
        } else {
            ScriptNode scriptNode = new ScriptNode();
            scriptNode.setGroupId(query.getGroupId());
            scriptNode.setServiceId(query.getServiceId());
            scriptNode.setInstanceId(query.getInstanceId());

            list = scriptNodeService.list(new QueryWrapper<>(
                    scriptNode
            ));
        }

        return list.stream().map(scriptNode -> new RegisteredScriptNode(scriptNode, true)).toList();
    }

    @Override
    public List<RegisteredScriptNode> queryServiceInstance(String key) {
        String[] split = key.split(ServerInstanceUtil.SERVICE_INSTANCE_KEY_DISPATCHER);
        return queryServiceInstance(
                ServiceInstance.builder().groupId(split[0]).serviceId(split[1]).instanceId(split[2]).build()
        );
    }

    @Override
    public List<RegisteredScriptNode> queryServiceInstance(String groupId, String serviceId, String clientId) {
        return queryServiceInstance(
                ServiceInstance.builder().groupId(groupId).serviceId(serviceId).instanceId(clientId).build()
        );
    }

    @Override
    public boolean existServiceInstance(String key) {
        String[] split = key.split(ServerInstanceUtil.SERVICE_INSTANCE_KEY_DISPATCHER);
        ScriptNode scriptNode = new ScriptNode();
        scriptNode.setGroupId(split[0]);
        scriptNode.setServiceId(split[1]);
        scriptNode.setInstanceId(split[2]);
        return scriptNodeService.exists(new QueryWrapper<>(scriptNode));
    }

    /**
     * 保存ScriptNode的application.yaml文件。由于需要远程配置，仅在没有的时候创建
     *
     * @param scriptNodeName scriptNodeName
     * @param nodeAppConfig  nodeAppConfig
     * @throws IOException IOException
     */
    private void trySaveRawScriptNodeApplicationConfig(String scriptNodeName, String nodeAppConfig) throws IOException {
        synchronized (scriptNodeName.intern()) {
            Path path = FileUtil.getScriptNodeConfig(scriptNodeName + File.separator + VortexaPlatFormConstants.SCRIPT_NODE_CONFIG_FILE);
            if (!Files.exists(path)) {
                YamlConfigLoadUtil.writeYamlFile(path, JSON.parseObject(nodeAppConfig, Map.class));
            }
        }
    }
}
