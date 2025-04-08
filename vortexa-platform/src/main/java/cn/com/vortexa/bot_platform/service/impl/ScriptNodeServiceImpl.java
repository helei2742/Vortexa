package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.bot_platform.script_control.BotPlatformControlServer;
import cn.com.vortexa.common.dto.control.RegisteredScriptNode;
import cn.com.vortexa.control.util.ControlServerUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.com.vortexa.bot_platform.service.IScriptNodeService;
import cn.com.vortexa.common.entity.ScriptNode;
import cn.com.vortexa.bot_platform.mapper.ScriptNodeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public List<RegisteredScriptNode> queryAllScriptNode() {
        List<ScriptNode> list = list();
        Set<String> onLines = new HashSet<>(botControlServer.getConnectionService().queryOnlineInstanceKey());
        return list.stream().map(scriptNode -> RegisteredScriptNode.builder()
                .scriptNode(scriptNode)
                .online(onLines.contains(ControlServerUtil.generateServiceInstanceKey(
                        scriptNode.getGroupId(), scriptNode.getServiceId(), scriptNode.getInstanceId()
                )))
                .build()
        ).toList();
    }
}
