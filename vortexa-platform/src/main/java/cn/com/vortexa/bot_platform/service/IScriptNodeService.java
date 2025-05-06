package cn.com.vortexa.bot_platform.service;

import cn.com.vortexa.bot_platform.vo.ScriptNodeDetail;
import cn.com.vortexa.bot_platform.vo.ScriptNodeVO;

import cn.com.vortexa.common.dto.Result;
import com.baomidou.mybatisplus.extension.service.IService;

import cn.com.vortexa.common.entity.ScriptNode;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
    List<ScriptNodeVO> queryAllScriptNode();

    /**
     * 根据scriptNodeName查找
     *
     * @param scriptNodeName scriptNodeName
     * @return ScriptNode
     */
    ScriptNode queryByScriptNodeName(String scriptNodeName);

    /**
     * 查详情， 包括节点bot的详细信息
     *
     * @param scriptNodeName scriptNodeName
     * @return ScriptNodeDetail
     */
    ScriptNodeDetail queryScriptNodeDetail(String scriptNodeName);

    /**
     * 加载script node 配置
     *
     * @param scriptNodeName scriptNodeName
     * @return String
     */
    String loadScriptNodeConfig(String scriptNodeName) throws IOException;

    /**
     * 加载script node 下运行的bot的启动配置
     *
     * @param scriptNodeName scriptNodeName
     * @param botKey         botKey
     * @return String
     */
    String loadScriptNodeBotLaunchConfig(String scriptNodeName, String botKey) throws IOException;

    /**
     * 更新script node 下的bot的启动配置
     *
     * @param scriptNodeName  scriptNodeName
     * @param botKey          botKey
     * @param botLaunchConfig botLaunchConfig
     */
    void updateScriptNodeBotLaunchConfig(String scriptNodeName, String botKey, String botLaunchConfig) throws IOException;

    /**
     * 启动bot
     *
     * @param scriptNodeName scriptNodeName
     * @param botKey         botKey
     * @return Result
     */
    Result startBot(String scriptNodeName, String botKey) throws ExecutionException, InterruptedException;

    /**
     * 关闭bot
     *
     * @param scriptNodeName scriptNodeName
     * @param botKey         botKey
     * @return Result
     */
    Result stopBot(String scriptNodeName, String botKey);
}
