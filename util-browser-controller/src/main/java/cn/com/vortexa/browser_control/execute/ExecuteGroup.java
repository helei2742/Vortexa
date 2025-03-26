package cn.com.vortexa.browser_control.execute;

import cn.com.vortexa.browser_control.SeleniumInstance;
import lombok.*;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.function.BiFunction;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteGroup {

    private String name;

    private List<ExecuteItem> executeItems;

    private BiFunction<WebDriver, SeleniumInstance, Boolean> enterCondition;

}
