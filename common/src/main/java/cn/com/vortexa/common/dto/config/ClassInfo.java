package cn.com.vortexa.common.dto.config;


import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author helei
 * @since 2025-04-05
 */
@Data
public class ClassInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = -39284793274893278L;
    private String className;

    private String classFileName;

    private String classFilePath;
}
