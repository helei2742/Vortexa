package cn.com.vortexa.common.entity;


import cn.com.vortexa.common.dto.ConnectStatusInfo;
import cn.com.vortexa.common.util.excel.IntegerStringConverter;
import cn.com.vortexa.common.util.propertylisten.PropertyChangeListenClass;
import cn.com.vortexa.common.util.propertylisten.PropertyChangeListenField;
import cn.com.vortexa.common.util.tableprinter.CommandTableField;
import cn.com.vortexa.common.util.typehandler.LocalDateTimeTypeHandler;
import cn.com.vortexa.common.util.typehandler.MapTextTypeHandler;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import lombok.*;

/**
 * <p>
 *
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@Getter
@Setter
@TableName("t_bot_account_context")
@NoArgsConstructor
@Builder
@AllArgsConstructor
@PropertyChangeListenClass(isDeep = true)
public class AccountContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 5648946541345416541L;

    @TableId(value = "id", type = IdType.AUTO)
    @CommandTableField
    private Integer id;

    @TableField("bot_id")
    @CommandTableField
    private Integer botId;

    @TableField("bot_key")
    @ExcelProperty(value = "bot_key")
    @CommandTableField
    private String botKey;

    @TableField("account_base_info_id")
    @ExcelProperty(value = "account_base_info_id", converter = IntegerStringConverter.class)
    @CommandTableField
    private Integer accountBaseInfoId;

    @TableField("twitter_id")
    @ExcelProperty(value = "twitter_id", converter = IntegerStringConverter.class)
    private Integer twitterId;

    @TableField("discord_id")
    @ExcelProperty(value = "discord_id", converter = IntegerStringConverter.class)
    private Integer discordId;

    @TableField("proxy_id")
    @ExcelProperty(value = "proxy_id", converter = IntegerStringConverter.class)
    private Integer proxyId;

    @TableField("browser_env_id")
    @ExcelProperty(value = "browser_env_id", converter = IntegerStringConverter.class)
    private Integer browserEnvId;

    @TableField("telegram_id")
    @ExcelProperty(value = "telegram_id", converter = IntegerStringConverter.class)
    private Integer telegramId;

    @TableField("wallet_id")
    @ExcelProperty(value = "wallet_id", converter = IntegerStringConverter.class)
    private Integer walletId;

    @TableField("reward_id")
    private Integer rewardId;

    /**
     * 账号状态
     * 0 表示初始状态
     * 1 表示已注册
     */
    @TableField("status")
    @PropertyChangeListenField
    @CommandTableField
    private Integer status;

    @JSONField(serialize = true, deserialize = true)
    @TableField(value = "params", typeHandler = MapTextTypeHandler.class)
    @PropertyChangeListenField
    private Map<String, Object> params = new HashMap<>();

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer valid;


    @TableField(exist = false)
    @PropertyChangeListenField
    private RewordInfo rewordInfo = new RewordInfo();

    @TableField(exist = false)
    private AccountBaseInfo accountBaseInfo;

    @TableField(exist = false)
    private TwitterAccount twitter;

    @TableField(exist = false)
    private DiscordAccount discord;

    @TableField(exist = false)
    private TelegramAccount telegram;

    @TableField(exist = false)
    @CommandTableField
    private ProxyInfo proxy;

    @TableField(exist = false)
    @CommandTableField
    private BrowserEnv browserEnv;

    @TableField(exist = false)
    private final ConnectStatusInfo connectStatusInfo = new ConnectStatusInfo();

    public String getParam(String key) {
        return params.get(key) == null ? "" : params.get(key).toString();
    }

    public <T> T getParam(String key, Supplier<T> build) {
        Object o = params.get(key);
        if (o == null) {
            o = build.get();
        }
        return o == null ? null : (T) o;
    }

    public void setParam(String key, Object value) {
        params.put(key, value);
    }

    public void removeParam(String key) {
        params.remove(key);
    }

    public String getName() {
        if (accountBaseInfo == null) return "";
        return accountBaseInfo.getName() == null ? accountBaseInfo.getEmail() : accountBaseInfo.getName();
    }

    public String getSimpleInfo() {
        return String.format("%s-账户[%s]-代理[%s]", getId(), getName(), getProxy() == null ? "NO_PROXY" : getProxy().generateAddressStr());
    }

    public Boolean isSignUp() {
        return status != null && status == 1;
    }

    public String getType() {
        return accountBaseInfo == null ? null : accountBaseInfo.getType();
    }

    public static void signUpSuccess(AccountContext accountContext) {
        accountContext.setStatus(1);
    }
}
