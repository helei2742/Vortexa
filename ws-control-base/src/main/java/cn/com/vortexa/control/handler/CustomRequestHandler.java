package cn.com.vortexa.control.handler;

import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.dto.RequestHandleResult;

/**
 * @author helei
 * @since 2025/3/20 11:09
 */
public interface CustomRequestHandler {

    RequestHandleResult handlerRequest(RemotingCommand request);

}
