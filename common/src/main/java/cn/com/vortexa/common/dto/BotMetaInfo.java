package cn.com.vortexa.common.dto;

import cn.com.vortexa.common.dto.job.AutoBotJobParam;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author com.helei
 * @since 2025/4/22 16:30
 */
@Data
public class BotMetaInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = -3498562379865872387L;

    /**
     * bot name，需要与对应类的@BotApplication注解中的name一致
     */
    private String botName;

    /**
     * 类名， 必填，还需包含包路径
     */
    private String className;

    /**
     * 其它需加载的类名
     */
    private List<String> extraClassNameList;

    /**
     * bot的job参数
     */
    private List<AutoBotJobParam> jobParams;

    /**
     * bot的描述
     */
    private String description;



    /**
     * bot版本
     */
    private String version;

    /**
     * 图标，base64字符串
     */
    private String icon;

    /**
     * 资源文件dir, 运行时写入，为class文件所在目录(bot-meta-config所在目录)
     */
    private String resourceDir;

    /**
     * class文件所在的jar包路径，运行时写入
     */
    private String classJarPath;
}
