package cn.com.vortexa.control.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author helei
 * @since 2025/3/20 14:18
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestHandleResult {

    private Boolean success;

    private Object data;

    public static RequestHandleResult success(Object data) {
        return new RequestHandleResult(true, data);
    }

    public static RequestHandleResult fail(Object data) {
        return new RequestHandleResult(false, data);
    }
}
