package cn.com.vortexa.bot_platform.controller;

import cn.com.vortexa.bot_platform.dto.BotJob;
import cn.com.vortexa.common.dto.control.RegisteredScriptNode;
import cn.com.vortexa.common.exception.BotStartException;
import cn.com.vortexa.common.vo.PageQuery;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.control.anno.RPCReference;
import cn.com.vortexa.bot_platform.service.IBotInstanceService;
import cn.com.vortexa.rpc.api.bot.IScriptAgentRPC;

import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @RPCReference
    private IScriptAgentRPC scriptAgentRPC;

    @PostMapping("/pageQuery")
    public Result pageQuery(@RequestBody PageQuery query) throws SQLException, SchedulerException {
        return Result.ok(botInstanceService.conditionPageQueryAllInfo(
                query.getPage(),
                query.getLimit(),
                query.getFilterMap()
        ));
    }

    @PostMapping("/onlineInstance")
    public Result onlineInstance() {
        List<RegisteredScriptNode> registeredScriptNodes = botInstanceService.queryOnLineInstance();
        return Result.ok(registeredScriptNodes);
    }

    @PostMapping("/startJob")
    public Result startJob(@RequestBody BotJob botJob) throws BotStartException {
        return botInstanceService.startJob(botJob);
    }
}
