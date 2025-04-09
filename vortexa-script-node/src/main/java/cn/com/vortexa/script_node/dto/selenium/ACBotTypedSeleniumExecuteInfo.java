package cn.com.vortexa.script_node.dto.selenium;

import cn.com.vortexa.browser_control.execute.ExecuteGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ACBotTypedSeleniumExecuteInfo {

    private String botKey;

    private List<ExecuteGroup> seleniumExecuteChain;

}
