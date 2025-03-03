package cn.com.helei.bot_father.websocket;


import cn.com.helei.common.entity.AccountContext;

import java.lang.reflect.InvocationTargetException;

public interface AccountWSClientBuilder {

    BaseBotWSClient<?, ?> build(AccountContext accountContext) throws InvocationTargetException, IllegalAccessException;

}
