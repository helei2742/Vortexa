

        package cn.com.vortexa.common.util;

import cn.com.vortexa.common.constants.FilePathType;

import java.io.*;
        import java.nio.file.Path;
import java.util.List;

public class FileUtil {

    public static final List<String> CONFIG_DIR_BOT_PATH = List.of("config", "bot");

    public static final List<String> CONFIG_DIR_APP_PATH = List.of("config", "app");

    /**
     * app资源根目录
     */
    public static final String RESOURCE_ROOT_DIR = System.getProperty("user.dir") + File.separator + "botData";

    /**
     * 获取配置文件
     *
     * @param path     子路径
     * @param fileName 文件名
     * @return 绝对路径
     */
    public static String getAppResourcePath(List<String> path, String fileName) {
        StringBuilder sb = new StringBuilder(RESOURCE_ROOT_DIR);

        for (String p : path) {
            sb.append(File.separator).append(p);
        }
        return sb.append(File.separator).append(fileName).toString();
    }

    /**
     * 获取app配置目录
     *
     * @return 配置目录绝对路径
     */
    public static String getAppResourceConfigPath() {
        return RESOURCE_ROOT_DIR + File.separator + "config";
    }

    /**
     * 获取app配置目录
     *
     * @return 配置目录绝对路径
     */
    public static String getAppResourceAppConfigPath() {
        return RESOURCE_ROOT_DIR + File.separator + String.join(File.separator, CONFIG_DIR_APP_PATH);
    }

    /**
     * 获取系统配置目录
     *
     * @return 配置目录绝对路径
     */
    public static String getAppResourceSystemConfigPath() {
        return RESOURCE_ROOT_DIR + File.separator + String.join(File.separator, CONFIG_DIR_BOT_PATH);
    }


    /**
     * 获取系统配置目录
     *
     * @return 配置目录绝对路径
     */
    public static String getAppResourceDataPath() {
        return RESOURCE_ROOT_DIR + File.separator + "data";
    }

    /**
     * 生成绝对路径
     *
     * @param patternPath             patternPath
     * @param botInstanceResourcePath botInstanceResourcePath
     * @return 绝对路径
     */
    public static String generateAbsPath(String patternPath, String botInstanceResourcePath) {
        FilePathType filePathType = FilePathType.resolveFilePathType(patternPath);
        return switch (filePathType) {
            case absolute -> {
                if (patternPath.startsWith(filePathType.name())) {
                    yield patternPath.replace("absolute:", "");
                }
                yield patternPath;
            }
            case instance_resource ->
                    patternPath.replace("instance_resource:", botInstanceResourcePath + File.separator);
            case app_resource -> patternPath.replace("app_resource:", RESOURCE_ROOT_DIR + File.separator);
            case app_resource_config ->
                    patternPath.replace("app_resource_config:", getAppResourceAppConfigPath() + File.separator);
            case app_resource_data ->
                    patternPath.replace("app_resource_data:", getAppResourceDataPath() + File.separator);
        };
    }

    /**
     * 保存
     */
    public static void saveJSONStringContext(Path filePath, String jsonContext) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            writer.write(jsonContext);
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
package cn.com.vortexa.common.entity;

import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.dto.control.ServiceInstance;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.*;

        import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

import lombok.*;

/**
 * <p>
 *
 * </p>
 *
 * @author com.helei
 * @since 2025-04-08
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_script_node")
@EqualsAndHashCode(callSuper = true)
public class ScriptNode extends ServiceInstance implements Serializable {
    @Serial
    private static final long serialVersionUID = -1023437865743897341L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("script_node_name")
    private String scriptNodeName;

    @TableField("description")
    private String description;

    @TableField("bot_config_map")
    private Map<String, AutoBotConfig> botConfigMap;

    @TableField("params")
    private Map<String, Object> params;

    @TableField(value = "insert_datetime", fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer valid;

    public boolean usable() {
        return StrUtil.isNotBlank(groupId) && StrUtil.isNotBlank(serviceId) && StrUtil.isNotBlank(instanceId)
                && StrUtil.isNotBlank(host) && port != null && StrUtil.isNotBlank(scriptNodeName);
    }

    public static ScriptNode generateFromServiceInstance(ServiceInstance serviceInstance) {
        ScriptNode scriptNode = new ScriptNode();
        scriptNode.setHost(serviceInstance.getHost());
        scriptNode.setPort(serviceInstance.getPort());
        scriptNode.setGroupId(serviceInstance.getGroupId());
        scriptNode.setServiceId(serviceInstance.getServiceId());
        scriptNode.setInstanceId(serviceInstance.getInstanceId());

        return scriptNode;
    }
}
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="cn.com.vortexa.bot_platform.mapper.ScriptNodeMapper">

    <!-- 通用查询映射结果 -->
    <resultMap id="BaseResultMap" type="cn.com.vortexa.common.entity.ScriptNode">
        <id column="id" property="id" />
        <result column="group_id" property="groupId" />
        <result column="service_id" property="serviceId" />
        <result column="instance_id" property="instanceId" />
        <result column="host" property="host" />
        <result column="port" property="port" />
        <result column="script_node_name" property="scriptNodeName" />
        <result column="description" property="description" />
        <result column="bot_config_map" property="botConfigMap" />
        <result column="params" property="params" />
        <result column="insert_datetime" property="insertDatetime" />
        <result column="update_datetime" property="updateDatetime" />
        <result column="valid" property="valid" />
    </resultMap>

    <insert id="insertOrUpdate">
INSERT INTO t_script_node
        (group_id, service_id, instance_id, host, port, script_node_name, description, bot_config_map, params, update_datetime,  valid)
VALUES (
                #{groupId}, #{serviceId}, #{instanceId}, #{host}, #{port}, #{scriptNodeName}, #{description}, #{botConfigMap}, #{params},  current_timestamp, 1
        ) ON DUPLICATE KEY  UPDATE
        <trim suffixOverrides=",">
            <if test="groupId != null">
group_id = values(group_id),
            </if>
            <if test="serviceId != null">
service_id = values(service_id),
            </if>
            <if test="instanceId != null">
instance_id = values(instance_id),
            </if>
            <if test="host != null">
host = values(host),
            </if>
            <if test="port != null">
port = values(port),
            </if>
            <if test="scriptNodeName != null">
script_node_name = values(script_node_name),
            </if>
            <if test="description != null">
description = values(description),
            </if>
            <if test="botConfigMap != null">
bot_config_map = values(bot_config_map),
            </if>
            <if test="params != null">
params = values(params),
            </if>
update_datetime = values(update_datetime),
valid = values(valid),
        </trim>
    </insert>
</mapper>
