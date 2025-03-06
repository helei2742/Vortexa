package cn.com.helei.example.haha_wallet;


import cn.com.helei.bot_father.BotLauncher;
import cn.com.helei.bot_father.anno.BotApplication;
import cn.com.helei.bot_father.anno.BotMethod;
import cn.com.helei.bot_father.bot.AutoLaunchBot;
import cn.com.helei.bot_father.config.AutoBotConfig;
import cn.com.helei.bot_father.service.BotApi;
import cn.com.helei.common.constants.BotJobType;
import cn.com.helei.common.entity.AccountContext;
import cn.com.helei.common.exception.BotInitException;
import cn.com.helei.common.exception.BotStartException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@BotApplication(
        name = "haha_selenium_bot",
        accountParams = {HaHaWalletBot.WALLET_KEY}
)
public class HaHaWalletBot extends AutoLaunchBot<HaHaWalletBot> {

    public static final String WALLET_KEY = "haha_wallet";

    public static final String USERNAME_KEY = "haha_username";

    public static final String PASSWORD_KEY = "haha_password";

    public static final String TODAY_COUNT_KEY = "haha_today_count";

    public static final String TODAY_KEY = "haha_today_count";

    @Override
    protected void botInitialized(AutoBotConfig botConfig, BotApi botApi) {
        System.setProperty("java.awt.headless", "false");
    }

    @Override
    protected HaHaWalletBot getInstance() {
        return this;
    }

    @BotMethod(jobType = BotJobType.TIMED_TASK, intervalInSecond = 60 * 60 * 24,
            dynamicTrigger = false, dynamicTimeWindowMinute = 60 * 5, syncExecute = true)
    public void dailyTask(AccountContext accountContext) throws IOException, InterruptedException {
        logger.info("[%s] start daily task".formatted(accountContext.getSimpleInfo()));

        HahaWalletSelenium hahaWalletSelenium = new HahaWalletSelenium(this, accountContext);
        hahaWalletSelenium.syncStart();

        logger.info("[%s] daily task finish".formatted(accountContext.getSimpleInfo()));
    }

    public static void main(String[] args) throws BotStartException, BotInitException {
        List<String> list = new ArrayList<>(List.of(args));
        list.add("--bot.botKey=haha_wallet_test");
        list.add("--bot.accountConfig.configFilePath=haha_wallet_google.xlsx");
        list.add("--add-opens java.base/java.lang=ALL-UNNAMED");

        BotLauncher.launch(HaHaWalletBot.class, list.toArray(new String[0]));
    }
}

