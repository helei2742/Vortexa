package cn.com.vortexa.browser_control.dto;


import cn.hutool.core.lang.Pair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author helei
 * @since 2025-04-05
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeleniumParams {

    /**
     * browser driver path
     */
    private String driverPath;

    /**
     * 目标网址
     */
    private String targetWebSite;

    /**
     * 浏览器选项
     */
    private List<String> chromeOptions;


    private List<Pair<String, String>> experimentalOptions;

    /**
     * 拓展路径
     */
    private List<String> extensionPaths;
}
