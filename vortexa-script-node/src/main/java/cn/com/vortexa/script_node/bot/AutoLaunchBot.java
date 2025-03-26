package cn.com.vortexa.script_node.bot;

import cn.com.vortexa.script_node.config.AutoBotConfig;
import cn.com.vortexa.script_node.constants.BotStatus;
import cn.com.vortexa.script_node.service.BotApi;
import cn.com.vortexa.script_node.view.MenuCMDLineAutoBot;
import cn.com.vortexa.common.exception.BotInitException;
import cn.com.vortexa.common.exception.BotStartException;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;

/**
 * @param <T>
 */
public abstract class AutoLaunchBot<T extends AnnoDriveAutoBot<T>> extends AnnoDriveAutoBot<T> {

    private static final Logger log = LoggerFactory.getLogger(AutoLaunchBot.class);

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
    public void launch(AutoBotConfig botConfig, BotApi botApi, Supplier<Boolean> initHandler) throws BotStartException, BotInitException {
        String botKey = botConfig.getBotKey();
        if (StrUtil.isBlank(botKey)) {
            throw new BotStartException("botKey is empty");
        }

        T instance = getInstance();

        // 初始化
        instance.init(botApi, botConfig);

        if (BooleanUtil.isTrue(initHandler.get())) {
            botInitialized(botConfig, botApi);

            if (botConfig.isCommandMenu()) {
                MenuCMDLineAutoBot<AutoBotConfig> menuCMDLineAutoBot
                        = new MenuCMDLineAutoBot<>(instance, List.of());

                // 启动
                menuCMDLineAutoBot.start();
                log.info("bot[{}] running as cli-ui mode", getBotInstance().getBotKey());
            } else {
                // 启动
                instance.updateState(BotStatus.RUNNING);
                log.info("bot[{}] running as headless mode", getBotInstance().getBotKey());
            }
        } else {
            log.error("bot start cancel by init");
        }
    }

    protected abstract void botInitialized(AutoBotConfig botConfig, BotApi botApi);
}
