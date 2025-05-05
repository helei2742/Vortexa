package cn.com.vortexa.common.dto.account;


import cn.com.vortexa.common.entity.AccountContext;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author helei
 * @since 2025-05-05
 */
@Data
public class BotInstanceAccount implements Serializable {
    @Serial
    private static final long serialVersionUID = 2308423945732842393L;
    private Integer id;
    private Integer botId;
    private String botKey;
    private Integer accountBaseInfoId;
    private Integer twitterId;
    private Integer discordId;
    private Integer proxyId;
    private Integer browserEnvId;
    private Integer telegramId;
    private Integer walletId;
    private Integer status;
    private Map<String, Object> params = new HashMap<>();
    private LocalDateTime insertDatetime;
    private LocalDateTime updateDatetime;


    public static BotInstanceAccount fromAccountContext(AccountContext accountContext) {
        BotInstanceAccount bot = new BotInstanceAccount();
        bot.id = accountContext.getId();
        bot.botId = accountContext.getBotId();
        bot.botKey = accountContext.getBotKey();
        bot.accountBaseInfoId = accountContext.getAccountBaseInfoId();
        bot.twitterId = accountContext.getTwitterId();
        bot.discordId = accountContext.getDiscordId();
        bot.proxyId = accountContext.getProxyId();
        bot.browserEnvId = accountContext.getBrowserEnvId();
        bot.telegramId = accountContext.getTelegramId();
        bot.walletId = accountContext.getWalletId();
        bot.status = accountContext.getStatus();
        bot.params = accountContext.getParams();
        bot.insertDatetime = accountContext.getInsertDatetime();
        bot.updateDatetime = accountContext.getUpdateDatetime();
        return bot;
    }

}
