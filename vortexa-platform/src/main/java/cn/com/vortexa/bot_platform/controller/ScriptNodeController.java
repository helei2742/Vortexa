package cn.com.vortexa.bot_platform.controller;

import cn.com.vortexa.bot_platform.service.IScriptNodeService;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.control.RegisteredScriptNode;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

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
            configStr = scriptNodeService.loadScriptNodeBotConfig(scriptNodeName, botKey);
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
        List<RegisteredScriptNode> list = scriptNodeService.queryAllScriptNode();
        return Result.ok(list);
    }
}
