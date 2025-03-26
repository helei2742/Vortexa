package cn.com.vortexa.script_node.websocket;


import cn.com.vortexa.common.entity.AccountContext;

import java.lang.reflect.InvocationTargetException;

public interface AccountWSClientBuilder {

    BaseBotWSClient<?> build(AccountContext accountContext) throws InvocationTargetException, IllegalAccessException;

}
