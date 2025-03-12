package cn.com.vortexa.nameserver.constant;


import io.netty.util.AttributeKey;

/**
 * @author helei
 * @since 2025-03-12
 */
public class NameserverSystemConstants {

    public static final int MAX_FRAME_LENGTH = 10240;

    public static final int MESSAGE_OBJ_POOL_INIT_SIZE = 100;

    public static final int MESSAGE_OBJ_POOL_MAX_SIZE = 500;

    public static final int EXPIRE_CLIENT_HANDLER_CLEAR_INTERVAL = 60000;

    public static final int RESPONSE_TIME_LIMIT = 60;

    /**
     * 放在netty channel 里的 client id 的 key
     */
    public static final AttributeKey<String> CLIENT_ID_KEY = AttributeKey.valueOf("clientId");

}
