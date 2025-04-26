package cn.com.vortexa.browser_control.execute;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteItem {

    private String name;

    private Integer retryTimes = 1;

    private Boolean errorSkip;

    private ExecuteLogic executeLogic;

    private ExecuteLogic resetLogic;
}
