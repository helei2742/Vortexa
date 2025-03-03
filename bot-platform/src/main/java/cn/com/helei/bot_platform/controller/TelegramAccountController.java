package cn.com.helei.bot_platform.controller;

import cn.com.helei.common.vo.BotImportVO;
import cn.com.helei.common.vo.DeleteVO;
import cn.com.helei.common.vo.PageQuery;
import cn.com.helei.common.dto.Result;
import cn.com.helei.rpc.ITelegramAccountRPC;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author com.helei
 * @since 2025-02-06
 */
@RestController
@RequestMapping("/telegram")
public class TelegramAccountController {

    @DubboReference
    private ITelegramAccountRPC telegramAccountRPC;

    @PostMapping("/batchAdd")
    public Result batchAdd(@RequestBody BotImportVO importVO) {
        return telegramAccountRPC.saveTelegrams(importVO.getRawLines());
    }

    @PostMapping("/pageQuery")
    public Result pageQuery(@RequestBody PageQuery query) throws SQLException {
        return Result.ok(telegramAccountRPC.conditionPageQuery(
                query.getPage(),
                query.getLimit(),
                query.getFilterMap()
        ));
    }

    @PostMapping("/delete")
    public Result delete(@RequestBody DeleteVO deleteVO) {
        Boolean delete = telegramAccountRPC.delete(deleteVO.getIds());
        if (delete) {
            return Result.ok();
        } else {
            return Result.fail("删除失败");
        }
    }
}
