package cn.com.vortexa.nameserver.handler;

import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.dto.RequestHandleResult;

/**
 * @author helei
 * @since 2025/3/20 11:09
 */
public interface CustomRequestHandler {

    RequestHandleResult handlerRequest(RemotingCommand request);

}
