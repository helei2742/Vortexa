package cn.com.vortexa.bot_platform.wsController;

import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author helei
 * @since 2025/4/1 14:27
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UIWSMessage {

    private String txId;

    private int code;

    private boolean success;

    private JSONObject params;

    private String message;

    private String errorMsg;
}
