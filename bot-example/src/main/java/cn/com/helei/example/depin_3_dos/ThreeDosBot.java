package cn.com.helei.example.depin_3_dos;

import cn.com.helei.bot_father.BotLauncher;
import cn.com.helei.bot_father.anno.BotApplication;
import cn.com.helei.bot_father.anno.BotMethod;
import cn.com.helei.bot_father.bot.AutoLaunchBot;
import cn.com.helei.bot_father.config.AutoBotConfig;
import cn.com.helei.bot_father.service.BotApi;
import cn.com.helei.common.constants.BotJobType;
import cn.com.helei.common.dto.Result;
import cn.com.helei.common.entity.AccountContext;
import cn.com.helei.common.exception.BotInitException;
import cn.com.helei.common.exception.BotStartException;

import java.util.List;

@BotApplication(
        name = "three_dos_bot",
        configParams = {"invite_code", ThreeDosApi.HARVESTED_DATA_KEY, ThreeDosApi.HARVESTED_URL_KEY},
        accountParams = {"password", "token"}
)
public class ThreeDosBot extends AutoLaunchBot<ThreeDosBot> {

    private ThreeDosApi threeDosApi;

    @Override
    protected void botInitialized(AutoBotConfig botConfig, BotApi botApi) {
        this.threeDosApi = new ThreeDosApi(this);
    }

    @Override
    protected ThreeDosBot getInstance() {
        return this;
    }

    @BotMethod(jobType = BotJobType.REGISTER, jobName = "自动注册")
    public Result autoRegister(AccountContext exampleAC, List<AccountContext> sameABIIdList, String inviteCode) {
        return threeDosApi.register(exampleAC, sameABIIdList, inviteCode);
    }

    @BotMethod(jobType = BotJobType.LOGIN, jobName = "自动获取token")
    public Result login(AccountContext accountContext) {
        return threeDosApi.login(accountContext);
    }

    @BotMethod(jobType = BotJobType.QUERY_REWARD, jobName = "奖励查询", intervalInSecond = 300, uniqueAccount = true)
    public Result queryReward(AccountContext exampleAC, List<AccountContext> sameABIIdList) {
        return threeDosApi.updateAccount(exampleAC, sameABIIdList);
    }

    @BotMethod(jobType = BotJobType.ONCE_TASK, jobName = "重发验证邮件", uniqueAccount = true)
    public void resendEmail(AccountContext exampleAC, List<AccountContext> sameABIIdList) {
        threeDosApi.resendEmail(exampleAC, sameABIIdList);
    }

    @BotMethod(jobType = BotJobType.ONCE_TASK, jobName = "验证邮箱")
    public void checkEmail(AccountContext accountContext) {
        threeDosApi.checkEmail(accountContext);
    }

    @BotMethod(jobType = BotJobType.ONCE_TASK, jobName = "生成秘钥")
    public void generateSecretKey(AccountContext accountContext) {
        threeDosApi.generateSecretKey(accountContext);
    }

    @BotMethod(
            jobType = BotJobType.TIMED_TASK,
            jobName = "每日登录",
            intervalInSecond = 60 * 60 * 12,
            uniqueAccount = true
    )
    public void dailyCheckIn(AccountContext exampleAC, List<AccountContext> sameABIIdList) {
        threeDosApi.dailyCheckIn(exampleAC, sameABIIdList);
    }

    @BotMethod(jobType = BotJobType.TIMED_TASK, jobName = "自动Ping", intervalInSecond = 60, dynamicTrigger = true, dynamicTimeWindowMinute = 300)
    public void keepAlivePing(AccountContext accountContext) {
        threeDosApi.keepLive(accountContext);
    }

    public static void main(String[] args) throws BotStartException, BotInitException {
        BotLauncher.launch(ThreeDosBot.class, args);
    }
}
