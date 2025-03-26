package cn.com.vortexa.browser_control.dto;


import com.alibaba.fastjson.JSONObject;

import cn.com.vortexa.common.constants.HttpMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.function.Function;

/**
 * @author helei
 * @since 2025/3/26 9:30
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QueryEntity<T> {
    private String contentPath;
    private HttpMethod method;
    private JSONObject body;
    private Function<String, T> resultStrHandler;
}
