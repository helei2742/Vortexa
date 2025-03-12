package cn.com.vortexa.bot_platform.controller;

import cn.com.vortexa.common.vo.BotImportVO;
import cn.com.vortexa.common.vo.DeleteVO;
import cn.com.vortexa.common.vo.PageQuery;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.rpc.IAccountBaseInfoRPC;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@RestController
@RequestMapping("/accountBaseInfo")
public class AccountBaseInfoController {

    @DubboReference
    private IAccountBaseInfoRPC accountBaseInfoRPC;

    @PostMapping("/batchAdd")
    public Result batchAdd(@RequestBody BotImportVO importVO) {
        return accountBaseInfoRPC.saveAccountBaseInfos(importVO.getRawLines());
    }

    @PostMapping("/pageQuery")
    public Result pageQuery(@RequestBody PageQuery query) throws SQLException {
        return Result.ok(accountBaseInfoRPC.conditionPageQuery(
                query.getPage(),
                query.getLimit(),
                query.getFilterMap()
        ));
    }

    @GetMapping("/typedInfo")
    public Result queryTypedInfo() {
        return accountBaseInfoRPC.queryTypedInfo();
    }

    @PostMapping("/delete")
    public Result delete(@RequestBody DeleteVO deleteVO) {
        Boolean delete = accountBaseInfoRPC.delete(deleteVO.getIds());
        if (delete) {
            return Result.ok();
        } else {
            return Result.fail("删除失败");
        }
    }
}
