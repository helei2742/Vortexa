package cn.com.vortexa.nameserver.util;

import java.io.File;

/**
 * @author helei
 * @since 2025/3/20 9:44
 */
public class NameserverUtil {

    private static final String STORE_FILE_RESOURCE_PATH = System.getProperty("user.dir") + File.separator
            + "nameserver";

    private static final String SERVICE_INSTANCE_KEY_DISPATCHER = "#%&%#";

    /**
     * 生成服务实例的key
     *
     * @param group group
     * @param serviceId serviceId
     * @param instanceId instanceId
     * @return String
     */
    public static String generateServiceInstanceKey(String group, String serviceId, String instanceId) {
        return group + SERVICE_INSTANCE_KEY_DISPATCHER + serviceId + SERVICE_INSTANCE_KEY_DISPATCHER + instanceId;
    }

    /**
     * 存储文件路径
     *
     * @param fileName fileName
     * @return path
     */
    public static String getStoreFileResourcePath(String fileName) {
        return STORE_FILE_RESOURCE_PATH + File.separator + fileName;
    }
}
