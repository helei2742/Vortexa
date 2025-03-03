package cn.com.helei.bot_platform.service.impl;

import cn.com.helei.common.config.SystemConfig;
import cn.com.helei.common.util.FileUtil;
import cn.com.helei.common.util.excel.ExcelReadUtil;
import cn.com.helei.db_layer.mapper.IBaseMapper;
import cn.com.helei.db_layer.service.AbstractBaseService;
import cn.com.helei.common.dto.Result;
import cn.com.helei.common.entity.TwitterAccount;
import cn.com.helei.db_layer.mapper.TwitterAccountMapper;
import cn.com.helei.rpc.ITwitterAccountRPC;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@Slf4j
@DubboService
public class TwitterAccountServiceImpl extends AbstractBaseService<TwitterAccountMapper, TwitterAccount> implements ITwitterAccountRPC {


    public TwitterAccountServiceImpl() {
        super(twitterAccount -> {
            twitterAccount.setInsertDatetime(LocalDateTime.now());
            twitterAccount.setUpdateDatetime(LocalDateTime.now());
            twitterAccount.setIsValid(1);
        });
    }

    @Override
    public Result saveTwitters(List<Map<String, Object>> rawLines) {
        if (rawLines == null || rawLines.isEmpty()) {
            return Result.fail("导入数据不能为空");
        }

        try {
            importFromRaw(rawLines);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("导入twitter 账号失败," + e.getMessage());
        }
    }

    @Override
    public Integer importFromExcel(String fileBotConfigPath) throws SQLException {
        String proxyFilePath = FileUtil.getConfigDirResourcePath(SystemConfig.CONFIG_DIR_BOT_PATH, fileBotConfigPath);

        try {
            List<Map<String, Object>> rawLines = ExcelReadUtil.readExcelToMap(proxyFilePath);

            return importFromRaw(rawLines);
        } catch (Exception e) {
            log.error("读取twitter account 文件[{}]发生异常", proxyFilePath, e);
            return 0;
        }
    }

    @Override
    public Integer importFromRaw(List<Map<String, Object>> rawLines) throws SQLException {
        List<TwitterAccount> twitterAccounts = rawLines.stream().map(map -> TwitterAccount.builder()
                .username(autoCast(map.remove("username")))
                .password(autoCast(map.remove("password")))
                .email(autoCast(map.remove("email")))
                .emailPassword(autoCast(map.remove("email_password")))
                .token(autoCast(map.remove("token")))
                .f2aKey(autoCast(map.remove("f2a_key")))
                .params(map)
                .build()).toList();

        return insertOrUpdateBatch(twitterAccounts);
    }
}
