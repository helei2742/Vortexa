package cn.com.vortexa.bot_father.websocket;

import cn.com.vortexa.common.entity.AccountContext;

import com.alibaba.fastjson.JSONObject;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

@Slf4j
@Setter
public abstract class BotJsonWSClient extends BaseBotWSClient<JSONObject> {

    private static final String idFieldName = "id";

    public BotJsonWSClient(
            AccountContext accountContext,
            String connectUrl,
            ExecutorService executorService
    ) {
        super(accountContext, connectUrl, new SimpleBotWSClientHandler(executorService));
        ((SimpleBotWSClientHandler) getHandler()).setWsClient(this);
    }

    @Setter
    @Getter
    private static class SimpleBotWSClientHandler extends BaseBotWSClientHandler<JSONObject> {

        private BotJsonWSClient wsClient;

        protected SimpleBotWSClientHandler(ExecutorService callbackInvoker) {
            super(callbackInvoker);
        }

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
