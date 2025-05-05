package cn.com.vortexa.bot_platform.controller;

import cn.com.vortexa.bot_platform.dto.BotInstanceAccountQuery;
import cn.com.vortexa.bot_platform.dto.BotJob;
import cn.com.vortexa.bot_platform.dto.BotInstanceUpdate;
import cn.com.vortexa.common.dto.control.RegisteredScriptNode;
import cn.com.vortexa.common.entity.BotInstance;
import cn.com.vortexa.common.vo.PageQuery;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.bot_platform.service.IBotInstanceService;

import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author com.helei
 * @since 2025-02-18
 */
@RestController
@RequestMapping("/botInstance")
public class BotInstanceController {

    @Autowired
    private IBotInstanceService botInstanceService;

    @PostMapping("/pageQuery")
    public Result pageQuery(@RequestBody PageQuery query) throws SQLException, SchedulerException {
        return Result.ok(botInstanceService.conditionPageQueryAllInfo(
                query.getPage(),
                query.getLimit(),
                query.getFilterMap()
        ));
    }

    @PostMapping("/detail")
    public Result detail(@RequestBody BotInstance botInstance) throws SchedulerException, IOException {
        return Result.ok(botInstanceService.detail(botInstance.getScriptNodeName(), botInstance.getBotKey()));
    }

    @PostMapping("/updateJobParam")
    public Result updateJobParam(@RequestBody BotInstanceUpdate botInstanceUpdate) {
        return botInstanceService.updateJobParam(botInstanceUpdate);
    }

    @PostMapping("/saveBotLaunchConfig")
    public Result saveBotLaunchConfig(@RequestBody BotInstanceUpdate update) throws IOException {
        return botInstanceService.saveBotInstanceLaunchConfig(
                update.getScriptNodeName(),
                update.getBotKey(),
                update.getBotLaunchConfig()
        );
    }

    @PostMapping("/onlineInstance")
    public Result onlineInstance() {
        List<RegisteredScriptNode> registeredScriptNodes = botInstanceService.queryOnLineInstance();
        return Result.ok(registeredScriptNodes);
    }

    @PostMapping("/startJob")
    public Result startJob(@RequestBody BotJob botJob) throws SchedulerException {
        return botInstanceService.startJob(botJob);
    }

    @PostMapping("/pauseJob")
    public Result pauseJob(@RequestBody BotJob botJob) throws SchedulerException {
        return botInstanceService.pauseJob(botJob);
    }

    @PostMapping("/deleteJob")
    public Result deleteJob(@RequestBody BotJob botJob) throws SchedulerException {
        return botInstanceService.deleteJob(botJob);
    }

    @PostMapping("/pageQueryAccount")
    public Result pageQueryAccount(@RequestBody BotInstanceAccountQuery accountQuery)  {
        return botInstanceService.conditionPageQueryAccount(accountQuery);
    }
}
