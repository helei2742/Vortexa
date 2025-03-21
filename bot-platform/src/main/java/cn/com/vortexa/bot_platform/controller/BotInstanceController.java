package cn.com.vortexa.bot_platform.controller;

import cn.com.vortexa.common.vo.PageQuery;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.db_layer.service.IBotInstanceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

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
    public Result pageQuery(PageQuery query) throws SQLException {
        return Result.ok(botInstanceService.conditionPageQuery(
                query.getPage(),
                query.getLimit(),
                query.getFilterMap()
        ));
    }
}
