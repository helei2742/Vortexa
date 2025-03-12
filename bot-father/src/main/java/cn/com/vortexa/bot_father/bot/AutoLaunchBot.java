package cn.com.vortexa.bot_father.bot;

import cn.com.vortexa.bot_father.config.AutoBotConfig;
import cn.com.vortexa.bot_father.service.BotApi;
import cn.com.vortexa.bot_father.view.MenuCMDLineAutoBot;
import cn.com.vortexa.common.exception.BotInitException;
import cn.com.vortexa.common.exception.BotStartException;
import cn.hutool.core.util.StrUtil;


import java.util.List;


/**
 * @param <T>
 */
public abstract class AutoLaunchBot<T extends AnnoDriveAutoBot<T>> extends AnnoDriveAutoBot<T> {


    @Override
    protected final void doInit() throws BotInitException {
        super.doInit();
    }

    /**
     * 启动Bot
     *
     * @param botConfig botConfig
     * @param botApi    botApi
     * @throws BotStartException BotStartException
     * @throws BotInitException  BotInitException
     */
    public void launch(AutoBotConfig botConfig, BotApi botApi) throws BotStartException, BotInitException {
        String botKey = botConfig.getBotKey();
        if (StrUtil.isBlank(botKey)) {
            throw new BotStartException("botKey is empty");
        }

        T instance = getInstance();

        // 初始化
        instance.init(botApi, botConfig);

        botInitialized(botConfig, botApi);

        if (botConfig.isCommandMenu()) {
            MenuCMDLineAutoBot<AutoBotConfig> menuCMDLineAutoBot
                    = new MenuCMDLineAutoBot<>(instance, List.of());

            // 启动
            menuCMDLineAutoBot.start();
        }
    }

    protected abstract void botInitialized(AutoBotConfig botConfig, BotApi botApi);
}
