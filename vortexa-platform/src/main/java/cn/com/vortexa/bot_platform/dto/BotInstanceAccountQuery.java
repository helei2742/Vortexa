package cn.com.vortexa.bot_platform.dto;


import cn.com.vortexa.common.vo.PageQuery;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author helei
 * @since 2025-05-05
 */
@Data
public class BotInstanceAccountQuery extends PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = -2192329837921837812L;

    private String scriptNodeName;
    private String botKey;
}
