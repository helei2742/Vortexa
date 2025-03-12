package cn.com.vortexa.bot_platform.controller;

import cn.com.vortexa.common.vo.BotBindVO;
import cn.com.vortexa.common.vo.PageQuery;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.rpc.IBotInfoRPC;
import org.apache.dubbo.config.annotation.DubboReference;
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

    @DubboReference
    private IBotInfoRPC botInfoRPC;

    @PostMapping("/pageQuery")
    public Result pageQuery(@RequestBody PageQuery query) throws SQLException {
        return Result.ok(botInfoRPC.conditionPageQuery(
                query.getPage(),
                query.getLimit(),
                query.getFilterMap()
        ));
    }

    @PostMapping("/create")
    public Result create(@RequestBody BotBindVO botBindVO) {
        return botInfoRPC.bindBotAccountBaseInfo(botBindVO.getBotId(), botBindVO.getBotKey(),
                botBindVO.getBindAccountBaseInfoList());
    }
}
