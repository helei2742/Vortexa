package cn.com.vortexa.script_node.bot;

import cn.com.vortexa.common.exception.BotStatusException;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.script_node.constants.BotStatus;
import cn.com.vortexa.script_node.service.BotApi;
import cn.com.vortexa.common.exception.BotInitException;
import cn.com.vortexa.common.exception.BotStartException;
import cn.com.vortexa.script_node.view.ScriptNodeCMDLineMenu;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @param <T>
 */
@Slf4j
@Setter
public abstract class AutoLaunchBot<T extends AnnoDriveAutoBot<T>> extends AnnoDriveAutoBot<T> {

    private BiConsumer<BotStatus, BotStatus> botStatusChangeHandler;

    @Override
    public synchronized void updateState(BotStatus newStatus) throws BotStatusException {
        BotStatus oldStatus = getStatus();
        super.updateState(newStatus);

        if (botStatusChangeHandler != null) {
            try {
                botStatusChangeHandler.accept(oldStatus, newStatus);
            } catch (Exception e) {
                log.error("bot state change handle error", e);
            }
        }
    }

    /**
     * 启动Bot
     *
     * @param botConfig botConfig
     * @param botApi    botApi
     * @throws BotStartException BotStartException
     * @throws BotInitException  BotInitException
     */
    public void launch(AutoBotConfig botConfig, BotApi botApi, Function<AutoLaunchBot<?>, Boolean> initHandler) throws BotStartException, BotInitException {
        String botKey = botConfig.getBotKey();
        if (StrUtil.isBlank(botKey)) {
            throw new BotStartException("botKey is empty");
        }
        T instance = getInstance();

        // 初始化
        instance.init(botApi, botConfig);

        instance.updateState(BotStatus.STARTING);
        if (BooleanUtil.isTrue(initHandler.apply(this))) {
            try {
                botInitialized(botConfig, botApi);
                instance.updateState(BotStatus.RUNNING);
            } catch (Exception e) {
                log.error("bot init error", e);
                instance.updateState(BotStatus.SHUTDOWN);
            }
        } else {
            logger.error("bot start cancel by init");
            instance.updateState(BotStatus.SHUTDOWN);
        }
    }

    protected abstract void botInitialized(AutoBotConfig botConfig, BotApi botApi);
}
