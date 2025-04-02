package cn.com.vortexa.example.klok;


import cn.com.vortexa.common.constants.BotJobType;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.common.exception.BotInitException;
import cn.com.vortexa.common.exception.BotStartException;
import cn.com.vortexa.script_node.ScriptNodeLauncher;
import cn.com.vortexa.script_node.anno.BotApplication;
import cn.com.vortexa.script_node.anno.BotMethod;
import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import cn.com.vortexa.script_node.config.AutoBotConfig;
import cn.com.vortexa.script_node.service.BotApi;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@BotApplication(
        name = "klok_bot",
        accountParams = {KlokBot.PRIMARY_KEY}
)
public class KlokBot extends AutoLaunchBot<KlokBot> {

    public static final String PRIMARY_KEY = "primary_key";
    public static final String ETH_ADDRESS = "eth_address";
    public static final String SESSION_TOKEN = "session_token";

    private KlokApi klokApi;

    @Override
    protected void botInitialized(AutoBotConfig botConfig, BotApi botApi) {
        klokApi = new KlokApi(this);
    }

    @Override
    protected KlokBot getInstance() {
        return this;
    }

    @BotMethod(jobType = BotJobType.LOGIN)
    public Result login(AccountContext accountContext) {
        if (accountContext.getId() != 1) return Result.fail("test error");
        return klokApi.login(accountContext);
    }

    public static void main(String[] args) throws BotStartException, BotInitException {
        List<String> list = new ArrayList<>(List.of(args));
        list.add("--vortexa.botKey=klok_test");
        list.add("--vortexa.customConfig.invite_code=KMDiFtp9");
        list.add("--vortexa.accountConfig.configFilePath=klok_google.xlsx");
        list.add("--add-opens java.base/java.lang=ALL-UNNAMED");

        ScriptNodeLauncher.launch(KlokBot.class, list.toArray(new String[0]));
    }
}

