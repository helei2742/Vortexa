package cn.com.vortexa.common.constants;


/**
 * @author helei
 * @since 2025-04-13
 */
public enum FilePathType {
    /**
     * 绝对路径
     */
    absolute,
    /**
     * bot 实例
     */
    instance_resource,
    /**
     * app路径
     */
    app_resource,
    /**
     * app路径下config路径
     */
    app_resource_config,
    /**
     * app路径下运行数据路径
     */
    app_resource_data,
    ;

    public static FilePathType resolveFilePathType(String filePath) {
        if (filePath.startsWith(absolute.name() + ":")) return absolute;
        else if (filePath.startsWith(instance_resource.name() + ":")) return instance_resource;
        else if (filePath.startsWith(app_resource.name() + ":")) return app_resource;
        else if (filePath.startsWith(app_resource_config.name() + ":")) return app_resource_config;
        else if (filePath.startsWith(app_resource_data.name() + ":")) return app_resource_data;
        else return absolute;
    }
}
