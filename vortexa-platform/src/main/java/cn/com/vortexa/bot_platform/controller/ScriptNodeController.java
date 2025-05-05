package cn.com.vortexa.bot_platform.controller;

import cn.com.vortexa.bot_platform.service.IScriptNodeService;
import cn.com.vortexa.bot_platform.vo.ScriptNodeVO;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.BotInstance;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author com.helei
 * @since 2025-04-08
 */
@RestController
@RequestMapping("/script-node")
public class ScriptNodeController {

    @Autowired
    private IScriptNodeService scriptNodeService;

    @PostMapping("/remote-config/{scriptNodeName}")
    public Result remoteConfig(@PathVariable("scriptNodeName") String scriptNodeName) {
        String configStr = null;
        try {
            configStr = scriptNodeService.loadScriptNodeConfig(scriptNodeName);
            if (StrUtil.isBlank(configStr)) {
                return Result.fail(scriptNodeName + " config is empty");
            }
            return Result.ok(configStr);
        } catch (IOException e) {
            return Result.fail(e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
        }
    }

    @PostMapping("/remote-config")
    public Result botRemoteConfig(
            @RequestParam("scriptNodeName") String scriptNodeName,
            @RequestParam("botKey") String botKey
    ) {
        String configStr = null;
        try {
            configStr = scriptNodeService.loadScriptNodeBotLaunchConfig(scriptNodeName, botKey);
            if (StrUtil.isBlank(configStr)) {
                return Result.fail(" config is empty");
            }
            return Result.ok(configStr);
        } catch (IOException e) {
            return Result.fail(e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
        }
    }

    @PostMapping("/all")
    public Result queryAllScriptNode() {
        List<ScriptNodeVO> list = scriptNodeService.queryAllScriptNode();
        return Result.ok(list);
    }

    @PostMapping("/start_bot")
    public Result startBot(@RequestBody BotInstance botInstance) throws ExecutionException, InterruptedException {
        return scriptNodeService.startBot(
                botInstance.getScriptNodeName(),
                botInstance.getBotKey()
        );
    }

    @PostMapping("/stop_bot")
    public Result stopBot(@RequestBody BotInstance botInstance) throws ExecutionException, InterruptedException {
        return scriptNodeService.stopBot(
                botInstance.getScriptNodeName(),
                botInstance.getBotKey()
        );
    }
}
