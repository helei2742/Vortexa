package cn.com.vortexa.bot_platform.controller;

import cn.com.vortexa.common.vo.BotImportVO;
import cn.com.vortexa.common.vo.DeleteVO;
import cn.com.vortexa.common.vo.PageQuery;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.rpc.ITwitterAccountRPC;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@RestController
@RequestMapping("/twitter")
public class TwitterAccountController {

    @DubboReference
    private ITwitterAccountRPC twitterAccountRPC;

    @PostMapping("/batchAdd")
    public Result batchAdd(@RequestBody BotImportVO importVO) {
        return twitterAccountRPC.saveTwitters(importVO.getRawLines());
    }

    @PostMapping("/pageQuery")
    public Result pageQuery(@RequestBody PageQuery query)  throws SQLException {
        return Result.ok(
                twitterAccountRPC.conditionPageQuery(
                        query.getPage(),
                        query.getLimit(),
                        query.getFilterMap()
                )
        );
    }


    @PostMapping("/delete")
    public Result delete(@RequestBody DeleteVO deleteVO) {
        Boolean delete = twitterAccountRPC.delete(deleteVO.getIds());
        if (delete) {
            return Result.ok();
        } else {
            return Result.fail("删除失败");
        }
    }
}
