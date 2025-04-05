package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.bot_platform.service.ITelegramAccountService;
import cn.com.vortexa.rpc.api.platform.ITelegramAccountRPC;
import cn.com.vortexa.common.config.SystemConfig;
import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.excel.ExcelReadUtil;
import cn.com.vortexa.db_layer.service.AbstractBaseService;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.TelegramAccount;
import cn.com.vortexa.bot_platform.mapper.TelegramAccountMapper;
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
 * @since 2025-02-06
 */
@Slf4j
@Service
public class TelegramAccountServiceImpl extends AbstractBaseService<TelegramAccountMapper, TelegramAccount> implements
        ITelegramAccountRPC, ITelegramAccountService {


    public TelegramAccountServiceImpl() {
        super(telegramAccount -> {
            telegramAccount.setInsertDatetime(LocalDateTime.now());
            telegramAccount.setUpdateDatetime(LocalDateTime.now());
            telegramAccount.setIsValid(1);
        });
    }


    @Override
    public Result saveTelegrams(List<Map<String, Object>> rawLines) {
        if (rawLines == null || rawLines.isEmpty()) {
            return Result.fail("导入数据不能为空");
        }

        try {
            importFromRaw(rawLines);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("导入telegram账号失败," + e.getMessage());
        }
    }

    @Override
    public Integer importFromExcel(String fileBotConfigPath) throws SQLException {
        String dirResourcePath = FileUtil.getConfigDirResourcePath(SystemConfig.CONFIG_DIR_BOT_PATH, fileBotConfigPath);

        try {
            List<Map<String, Object>> rawLines = ExcelReadUtil.readExcelToMap(dirResourcePath);

            return importFromRaw(rawLines);
        } catch (Exception e) {
            log.error("读取telegram account 文件[{}]发生异常", dirResourcePath, e);
            return 0;
        }
    }

    @Override
    public Integer importFromRaw(List<Map<String, Object>> rawLines) throws SQLException {
        List<TelegramAccount> telegramAccounts = rawLines.stream().map(map -> TelegramAccount.builder()
                .username(autoCast(map.remove("username")))
                .password(autoCast(map.remove("password")))
                .phonePrefix(autoCast(map.remove("phone_prefix")))
                .phone(autoCast(map.remove("phone")))
                .token(autoCast(map.remove("token")))
                .params(map)
                .build()
        ).toList();

        return insertOrUpdateBatch(telegramAccounts);
    }

    @Override
    public List<TelegramAccount> batchQueryByIdsRPC(List<Serializable> ids) {
        return super.batchQueryByIds(ids);
    }

    @Override
    public TelegramAccount queryByIdRPC(Serializable id) {
        return queryById(id);
    }

    @Override
    public PageResult<TelegramAccount> conditionPageQueryRPC(int page, int limit, Map<String, Object> filterMap)
            throws SQLException {
        return super.conditionPageQuery(page, limit, filterMap);
    }
}
