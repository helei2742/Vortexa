package cn.com.vortexa.bot_platform.controller;

import cn.com.vortexa.common.entity.BotInfo;
import cn.com.vortexa.common.vo.PageQuery;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.bot_platform.service.IBotInfoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author com.helei
 * @since 2025-02-07
 */
@RestController
@RequestMapping("/botInfo")
public class BotInfoController {

    @Autowired
    private IBotInfoService botInfoService;

    @PostMapping("/pageQuery")
    public Result pageQuery(@RequestBody PageQuery query) throws SQLException {
        return Result.ok(botInfoService.conditionPageQuery(
                query.getPage(),
                query.getLimit(),
                query.getFilterMap()
        ));
    }

    @PostMapping("/queryByName")
    public Result queryByName(@RequestBody BotInfo botInfo) throws SQLException {
        BotInfo bi = botInfoService.queryByName(botInfo.getName());
        return bi == null ? Result.fail("bot not found") : Result.ok(bi);
    }
}
