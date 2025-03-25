package cn.com.vortexa.example.stork_bot;

import cn.com.vortexa.bot_father.BotLauncher;
import cn.com.vortexa.bot_father.anno.BotApplication;
import cn.com.vortexa.bot_father.anno.BotMethod;
import cn.com.vortexa.bot_father.bot.AutoLaunchBot;
import cn.com.vortexa.bot_father.config.AutoBotConfig;
import cn.com.vortexa.bot_father.service.BotApi;
import cn.com.vortexa.common.constants.BotJobType;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.common.exception.BotInitException;
import cn.com.vortexa.common.exception.BotStartException;

import java.util.ArrayList;
import java.util.List;

import static cn.com.vortexa.example.stork_bot.StorkBotAPI.PASSWORD_KEY;

@BotApplication(name = "stork_bot", accountParams = PASSWORD_KEY)
public class StorkBot extends AutoLaunchBot<StorkBot> {


    private StorkBotAPI storkBotAPI;

    @Override
    protected void botInitialized(AutoBotConfig botConfig, BotApi botApi) {
        storkBotAPI = new StorkBotAPI(this);
    }

    @Override
    protected StorkBot getInstance() {
        return this;
    }

    @BotMethod(jobType = BotJobType.REGISTER)
    public Result signUp(AccountContext exampleAC, List<AccountContext> sameABIList, String inviteCode) {
        return storkBotAPI.signup(exampleAC, sameABIList, inviteCode);
    }

    @BotMethod(jobType = BotJobType.TIMED_TASK, intervalInSecond = 60 * 20)
    public void tokenRefresh(AccountContext accountContext) {
        storkBotAPI.refreshToken(accountContext);
    }

    @BotMethod(jobType = BotJobType.TIMED_TASK, intervalInSecond = 60 * 5)
    public void keepAlive(AccountContext accountContext) {
        storkBotAPI.keepAlive(accountContext);
    }

    public static void main(String[] args) throws BotStartException, BotInitException {
        List<String> list = new ArrayList<>(List.of(args));

        list.add("--vortexa.botKey=stork_test");
        list.add("--vortexa.customConfig.invite_code=WSJQRJD5CB");
        list.add("--vortexa.accountConfig.configFilePath=stork_google.xlsx");
        list.add("--add-opens java.base/java.lang=ALL-UNNAMED");

        BotLauncher.launch(StorkBot.class, list.toArray(new String[0]));
    }

}
