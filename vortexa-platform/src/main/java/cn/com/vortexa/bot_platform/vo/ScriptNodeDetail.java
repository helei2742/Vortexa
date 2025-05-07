package cn.com.vortexa.bot_platform.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @author helei
 * @since 2025-05-06
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScriptNodeDetail {

    private ScriptNodeVO scriptNode;

    private Map<String, List<String>> botNameToBotKeys;

    private Map<String, List<String>> onlineBotNameToKeys;
}
