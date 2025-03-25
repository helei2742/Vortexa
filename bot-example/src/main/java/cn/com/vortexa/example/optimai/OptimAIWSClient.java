package cn.com.vortexa.example.optimai;

import com.alibaba.fastjson.JSONObject;

import cn.com.vortexa.bot_father.websocket.BotJsonWSClient;
import cn.com.vortexa.common.entity.AccountContext;

/**
 * @author h30069248
 * @since 2025/3/24 17:04
 */
public class OptimAIWSClient extends BotJsonWSClient {

    public OptimAIWSClient(AccountContext accountContext, String connectUrl) {
        super(accountContext, connectUrl);
    }

    @Override
    public JSONObject getHeartbeatMessage() {
        return new JSONObject();
    }

    @Override
    public void whenAccountReceiveResponse(Object id, JSONObject response) {

    }

    @Override
    public void whenAccountReceiveMessage(JSONObject message) {

    }
}
