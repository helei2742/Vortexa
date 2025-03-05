package cn.com.helei.browser_control;

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
