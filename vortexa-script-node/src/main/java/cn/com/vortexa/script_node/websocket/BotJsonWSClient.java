package cn.com.vortexa.script_node.websocket;

import cn.com.vortexa.common.entity.AccountContext;

import com.alibaba.fastjson.JSONObject;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Setter
public abstract class BotJsonWSClient extends BaseBotWSClient<JSONObject> {

    private static final String idFieldName = "id";

    public BotJsonWSClient(
            AccountContext accountContext,
            String connectUrl
    ) {
        super(accountContext, connectUrl, new SimpleBotWSClientHandler());
        ((SimpleBotWSClientHandler) getHandler()).setWsClient(this);
    }

    @Setter
    @Getter
    private static class SimpleBotWSClientHandler extends BaseBotWSClientHandler<JSONObject> {

        private BotJsonWSClient wsClient;

        @Override
        public JSONObject convertMessageToRespType(String message) {
            return JSONObject.parseObject(message);
        }


        @Override
        protected Object getMessageId(JSONObject message) {
            return message.get(idFieldName);
        }
    }
}
