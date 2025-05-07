package cn.com.vortexa.bot_platform.vo;

import cn.com.vortexa.common.entity.ScriptNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author helei
 * @since 2025-05-03
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScriptNodeVO {
    private Long id;    // 节点id
    private String scriptNodeName;  //  节点名
    private String description; //  表述
    private String groupId;
    private String serviceId;
    private String instanceId;
    private String host;
    private Integer port;
    private Boolean online; //  是否在线
    private List<String> managedBotKeyList; //  管理的botKey列表
    private LocalDateTime insertDatetime;
    private LocalDateTime updateDatetime;

    public static ScriptNodeVO of(ScriptNode scriptNode, boolean online, List<String> managedBotKeyList) {
        ScriptNodeVO scriptNodeVO = new ScriptNodeVO();
        scriptNodeVO.setId(scriptNode.getId());
        scriptNodeVO.setScriptNodeName(scriptNode.getScriptNodeName());
        scriptNodeVO.setDescription(scriptNode.getDescription());
        scriptNodeVO.setGroupId(scriptNode.getGroupId());
        scriptNodeVO.setServiceId(scriptNode.getServiceId());
        scriptNodeVO.setInstanceId(scriptNode.getInstanceId());
        scriptNodeVO.setHost(scriptNode.getHost());
        scriptNodeVO.setPort(scriptNode.getPort());
        scriptNodeVO.setOnline(online);
        scriptNodeVO.setManagedBotKeyList(managedBotKeyList);
        scriptNodeVO.setInsertDatetime(scriptNode.getInsertDatetime());
        scriptNodeVO.setUpdateDatetime(scriptNode.getUpdateDatetime());
        return scriptNodeVO;
    }
}
