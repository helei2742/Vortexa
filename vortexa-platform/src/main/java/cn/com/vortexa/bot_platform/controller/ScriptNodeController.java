package cn.com.vortexa.bot_platform.controller;

import cn.com.vortexa.bot_platform.service.IScriptNodeService;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.control.RegisteredScriptNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 *  前端控制器
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

    @PostMapping("/all")
    public Result queryAllScriptNode() {
        List<RegisteredScriptNode> list = scriptNodeService.queryAllScriptNode();
        return Result.ok(list);
    }
}
