//package cn.com.helei.example.threedos;
//
//
//import cn.com.helei.bot_father.service.BotApi;
//import cn.com.helei.bot_father.view.MenuCMDLineAutoBot;
//import cn.com.helei.bot_father.view.commandMenu.DefaultMenuType;
//import cn.com.helei.bot_father.constants.MapConfigKey;
//import cn.com.helei.bot_father.dto.AutoBotAccountConfig;
//import cn.com.helei.bot_father.config.AutoBotConfig;
//import cn.com.helei.common.exception.BotInitException;
//import cn.com.helei.common.exception.BotStartException;
//import cn.com.helei.example.depin_3_dos.ThreeDosApi;
//import cn.com.helei.example.depin_3_dos.ThreeDosBot;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.List;
//
//@SpringBootTest(classes = AutoBotApplication.class)
//class ThreeDosBotTest {
//    private ThreeDosBot threeDosBot;
//
//    private MenuCMDLineAutoBot<AutoBotConfig> menuCMDLineAutoBot;
//
//    private AutoBotConfig autoBotConfig;
//
//    private static final String HARVESTED_DATA = "Whoa, everyone’s rushing 3DOS at once! Our servers need a quick breather—hang tight and thanks for your patience!\n\nClose banner\nDashboard\nReferral Program\nEarn More Points\nFree Airdrops\nWhitelisted Manufacturers\nSupport\nFOR 3D PRINTER OWNERS\nConnect 3D Printer\nUpload and Print\nSearch Designs\nMarketplace\nFOLLOW US\n@3DOSNetwork\nJoin Discord\nJoin Telegram\nToggle Sidebar\nDashboard\n\nReferrals\n0\n\nRefer & Earn\nDownload Extension\nToggle theme\nRun the 3DOS™ AI Extension & Earn!\n\nEarn 100 Points everytime you tweet about 3DOS.\n\n  Tweet and Earn\nToday's Earnings\n\nPoints are updated every 3 hours\n\n3DOS Earnings\n3DOS Community Counter\nStatistics\nTime-Limited\nGet 1000 Points\nFollow us on X (Twitter)\nFollow\nFollow us on twitter\nTime-Limited\n\nPoints will be credited in 24 hours\n\nGet 1000 Points\nFollow us on Discord\nJoin\nTime-Limited\n\nPoints will be credited in 24 hours\n\nGet 1000 Points\nFollow us on Telegram\nJoin\n3DOS Leaderboard\nSr Number\tWallet address\tTotal points\tTotal Referrals\n\t\nNo results.\nClaim daily 3DOS reward\nHow to use 3DOS application\nUpdate user profile\nUpdate profile\nHow to Earn Points with 3DOS?\n1. Referral\n\nHead to the Referral Page and invite all your friends—more invites, more rewards! 🚀\n\n2. Chrome Extension\n\nGenerate your API key, download the 3DOS Chrome Extension, and use the key to log in! Keep running node and start earning rewards! 🔑🚀\n\n3. Daily Rewards\n\nLog in daily and claim your exclusive daily rewards! 🎁✨ Don’t miss out!";
//
//    private static final String HARVESTED_URL = "https://dashboard.3dos.io/home/dashboard";
//
//    @Autowired
//    public BotApi botApi;
//
//    @BeforeEach
//    public void setUp() throws BotStartException {
//        autoBotConfig = new AutoBotConfig();
//        autoBotConfig.setConfig(MapConfigKey.INVITE_CODE_KEY, "ecbfae");
//        autoBotConfig.setConfig(ThreeDosApi.HARVESTED_URL_KEY, HARVESTED_URL);
//        autoBotConfig.setConfig(ThreeDosApi.HARVESTED_DATA_KEY, HARVESTED_DATA);
//    }
//
//    @Test
//    void google() throws BotStartException, BotInitException {
//        autoBotConfig.getFilePathConfig().setBaseAccountFileBotConfigPath("base_account.xlsx");
//        AutoBotAccountConfig accountConfig = new AutoBotAccountConfig();
//        accountConfig.setConfigFilePath("3dos/3dos_google.xlsx");
//        autoBotConfig.setAccountConfig(accountConfig);
//        autoBotConfig.setBotKey("3Mods-Google");
//
//        threeDosBot = new ThreeDosBot(autoBotConfig, botApi);
//
//        menuCMDLineAutoBot = new MenuCMDLineAutoBot<>(threeDosBot, List.of(DefaultMenuType.IMPORT));
//
//        menuCMDLineAutoBot.start();
//    }
//
//
//    @Test
//    void xinglan() throws BotStartException, BotInitException {
//        autoBotConfig.getFilePathConfig().setBaseAccountFileBotConfigPath("xinglan_base_account.xlsx");
//
//        AutoBotAccountConfig accountConfig = new AutoBotAccountConfig();
//        accountConfig.setConfigFilePath("3dos/3dos_xinglan.xlsx");
//        autoBotConfig.setAccountConfig(accountConfig);
//        autoBotConfig.setBotKey("3Mods-Xinglan");
//        threeDosBot = new ThreeDosBot(autoBotConfig, botApi);
//
//        menuCMDLineAutoBot = new MenuCMDLineAutoBot<>(threeDosBot, List.of(DefaultMenuType.IMPORT));
//
//        menuCMDLineAutoBot.start();
//    }
//}
