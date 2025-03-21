package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.db_layer.service.ITwitterAccountService;
import cn.com.vortexa.rpc.ITwitterAccountRPC;
import cn.com.vortexa.common.config.SystemConfig;
import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.excel.ExcelReadUtil;
import cn.com.vortexa.db_layer.service.AbstractBaseService;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.TwitterAccount;
import cn.com.vortexa.db_layer.mapper.TwitterAccountMapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.io.Serializable;
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
@Service
public class TwitterAccountServiceImpl extends AbstractBaseService<TwitterAccountMapper, TwitterAccount>
        implements ITwitterAccountRPC, ITwitterAccountService {

    public TwitterAccountServiceImpl() {
        super(twitterAccount -> {
            twitterAccount.setInsertDatetime(LocalDateTime.now());
            twitterAccount.setUpdateDatetime(LocalDateTime.now());
            twitterAccount.setIsValid(1);
        });
    }

    @Override
    public TwitterAccount queryByIdRPC(Serializable id) {
        return super.queryById(id);
    }

    @Override
    public PageResult<TwitterAccount> conditionPageQueryRPC(int page, int limit, Map<String, Object> filterMap)
            throws SQLException {
        return super.conditionPageQuery(page, limit, filterMap);
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
