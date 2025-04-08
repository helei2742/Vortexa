package cn.com.vortexa.control.dto;


import cn.com.vortexa.common.dto.control.RegisteredScriptNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author helei
 * @since 2025-03-12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceInstanceVO {
    private List<RegisteredScriptNode> instances;
}
