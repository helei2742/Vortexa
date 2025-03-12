package cn.com.vortexa.common.dto;


import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ACListOptResult {

    private Integer botId;

    private String botName;

    private String jobName;

    private Boolean success;

    private String errorMsg;

    private List<BotACJobResult> results;

    private Integer successCount;

    public static ACListOptResult fail(
            Integer botId,
            String botName,
            String jobName,
            String errorMsg
    ) {
        return new ACListOptResult(botId, botName, jobName, false, errorMsg, null, 0);
    }

    public String printStr() {
        return """
                bot: %s
                jobName: %s
                success: %s
                errorMsg: %s
                results: %s
                """.formatted(botName, jobName, success, errorMsg,
                results == null ? "" : results.stream().map(JSONObject::toJSONString).collect(Collectors.joining("\n")));
    }
}
