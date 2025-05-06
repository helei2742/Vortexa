package cn.com.vortexa.bot_platform.controller;


import cn.com.vortexa.common.dto.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author helei
 * @since 2025-05-06
 */
@RestController
@RequestMapping("/version")
public class VersionController {

    @PostMapping("/botVersions")
    public Result botVersions(@RequestBody List<String> botNames) {
        // TODO 查询bot的版本
        return Result.ok();
    }
}
