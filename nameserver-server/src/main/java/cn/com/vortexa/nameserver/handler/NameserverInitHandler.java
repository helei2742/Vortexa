package cn.com.vortexa.nameserver.handler;


import java.util.Map;

/**
 * @author helei
 * @since 11
 */
public class NameserverInitHandler implements ClientInitMsgHandler {
    @Override
    public boolean initClient(Map<String, String> prop) {
        return false;
    }
}
