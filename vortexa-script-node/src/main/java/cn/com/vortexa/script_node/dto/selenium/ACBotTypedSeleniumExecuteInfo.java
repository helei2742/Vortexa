package cn.com.vortexa.script_node.dto.selenium;

import cn.com.vortexa.browser_control.execute.ExecuteGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ACBotTypedSeleniumExecuteInfo {

    private String botKey;

    private String jobName;

    private Integer waitTime;

    private TimeUnit waitTimeUnit;

    private List<ExecuteGroup> seleniumExecuteChain;

}
