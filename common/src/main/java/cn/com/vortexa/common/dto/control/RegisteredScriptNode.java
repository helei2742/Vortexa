package cn.com.vortexa.common.dto.control;


import cn.com.vortexa.common.entity.ScriptNode;
import lombok.*;

/**
 * @author helei
 * @since 2025-03-12
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RegisteredScriptNode {

    private ScriptNode scriptNode;

    private boolean online;

    public RegisteredScriptNode(ScriptNode scriptNode) {
        this.scriptNode = scriptNode;
        this.online = false;
    }
}
