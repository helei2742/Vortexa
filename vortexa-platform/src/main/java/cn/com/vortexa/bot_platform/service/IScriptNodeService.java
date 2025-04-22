package cn.com.vortexa.bot_platform.service;

import cn.com.vortexa.common.dto.control.RegisteredScriptNode;

import com.baomidou.mybatisplus.extension.service.IService;

import cn.com.vortexa.common.entity.ScriptNode;

import java.io.IOException;
import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author com.helei
 * @since 2025-04-08
 */
public interface IScriptNodeService extends IService<ScriptNode> {

    Boolean insertOrUpdate(ScriptNode scriptNode);

    /**
     * 查询全部
     *
     * @return List<RegisteredScriptNode>
     */
    List<RegisteredScriptNode> queryAllScriptNode();

    /**
     * 加载script node 配置
     *
     * @param nodeId nodeId
     * @return String
     */
    String loadScriptNodeConfig(String nodeId) throws IOException;

    /**
     * 加载script node 下运行的bot配置
     *
     * @param scriptNodeName scriptNodeName
     * @param botKey botKey
     * @return String
     */
    String loadScriptNodeBotConfig(String scriptNodeName, String botKey) throws IOException;
}
