package cn.com.vortexa.bot_platform.controller;

import cn.com.vortexa.common.vo.BotBindVO;
import cn.com.vortexa.common.vo.PageQuery;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.db_layer.service.IBotInfoService;

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
@RequestMapping("/bot")
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

    @PostMapping("/create")
    public Result create(@RequestBody BotBindVO botBindVO) {
        return botInfoService.bindBotAccountBaseInfo(botBindVO.getBotId(), botBindVO.getBotKey(),
                botBindVO.getBindAccountBaseInfoList());
    }
}
