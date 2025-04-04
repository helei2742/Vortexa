package cn.com.vortexa.common.dto;


import cn.com.vortexa.common.dto.config.AutoBotConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * @author helei
 * @since 2025-04-04
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScriptNodeRegisterInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = -2735672328627372532L;

    private Map<String, AutoBotConfig> botKeyConfigMap;
}
