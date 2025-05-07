package cn.com.vortexa.common.util;

/**
 * @author helei
 * @since 2025/3/20 9:44
 */
public class ServerInstanceUtil {
    public static final String SERVICE_INSTANCE_KEY_DISPATCHER = "#%&%#";

    /**
     * 生成服务实例的key
     *
     * @param group      group
     * @param serviceId  serviceId
     * @param instanceId instanceId
     * @return String
     */
    public static String generateServiceInstanceKey(String group, String serviceId, String instanceId) {
        return group + SERVICE_INSTANCE_KEY_DISPATCHER + serviceId + SERVICE_INSTANCE_KEY_DISPATCHER + instanceId;
    }
}
