package cn.com.vortexa.bot_platform.controller;


import cn.com.vortexa.bot_platform.service.IVersionService;
import cn.com.vortexa.bot_platform.vo.BotVersionVO;
import cn.com.vortexa.common.dto.Result;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private IVersionService versionService;

    @PostMapping("/botVersions")
    public Result botVersions(@RequestBody BotVersionVO botVersionVO) {
        return Result.ok(versionService.queryBotNewestVersions(botVersionVO.getBotNames()));
    }
}
