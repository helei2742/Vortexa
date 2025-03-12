package cn.com.vortexa.bot_father.websocket;


import cn.com.vortexa.common.entity.AccountContext;

import java.lang.reflect.InvocationTargetException;

public interface AccountWSClientBuilder {

    BaseBotWSClient<?, ?> build(AccountContext accountContext) throws InvocationTargetException, IllegalAccessException;

}
