package cn.com.vortexa.example.klok;


import cn.com.vortexa.common.constants.BotJobType;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.common.exception.BotInitException;
import cn.com.vortexa.common.exception.BotStartException;
import cn.com.vortexa.script_node.ScriptAppLauncher;
import cn.com.vortexa.script_node.anno.BotApplication;
import cn.com.vortexa.script_node.anno.BotMethod;
import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import cn.com.vortexa.script_node.config.AutoBotConfig;
import cn.com.vortexa.script_node.service.BotApi;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@BotApplication(
        name = "klok_bot",
        accountParams = {KlokBot.PRIMARY_KEY}
)
public class KlokBot extends AutoLaunchBot<KlokBot> {

    public static final String PRIMARY_KEY = "primary_key";
    public static final String ETH_ADDRESS = "eth_address";
    public static final String SESSION_TOKEN = "session_token";
    public static final String DAILY_TIMES = "daily_times";
    public static final String DAILY_LIMIT = "daily_limit";

    private KlokApi klokApi;

    @Override
    protected void botInitialized(AutoBotConfig botConfig, BotApi botApi) {
        klokApi = new KlokApi(this);
    }

    @Override
    protected KlokBot getInstance() {
        return this;
    }

    @BotMethod(jobType = BotJobType.REGISTER, concurrentCount = 5)
    public Result register(AccountContext exampleAC, List<AccountContext> sameBAIDList, String inviteCode) {
        return klokApi.register(exampleAC, inviteCode);
    }

    @BotMethod(jobType = BotJobType.LOGIN, concurrentCount = 5)
    public Result login(AccountContext accountContext) {
        if (accountContext.getAccountBaseInfoId() != 3) return Result.fail("");
        return klokApi.login(accountContext);
    }

    @BotMethod(jobType = BotJobType.QUERY_REWARD, intervalInSecond = 24 * 60 * 60)
    public Result rewordQuery(AccountContext exampleAC, List<AccountContext> sameBAIDList) {
        return klokApi.rewordQuery(exampleAC);
    }

    @BotMethod(jobType = BotJobType.TIMED_TASK, intervalInSecond = 6 * 60 * 60, concurrentCount = 10)
    public void dailyTask(AccountContext accountContext) throws ExecutionException, InterruptedException {
        klokApi.dailyTask(accountContext);
    }

    public static void main(String[] args) throws BotStartException, BotInitException {
        List<String> list = new ArrayList<>(java.util.List.of(args));
        list.add("--vortexa.botKey=klok_test");
        list.add("--vortexa.customConfig.invite_code=TJXGVPJT");
        list.add("--vortexa.accountConfig.configFilePath=klok_google.xlsx");
        list.add("--add-opens java.base/java.lang=ALL-UNNAMED");

        ScriptAppLauncher.launch(KlokBot.class, list.toArray(new String[0]));
    }
}
