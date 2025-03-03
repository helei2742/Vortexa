package cn.com.helei.bot_platform.service.impl;

import cn.com.helei.common.config.SystemConfig;
import cn.com.helei.common.util.FileUtil;
import cn.com.helei.common.util.excel.ExcelReadUtil;
import cn.com.helei.db_layer.mapper.IBaseMapper;
import cn.com.helei.db_layer.service.AbstractBaseService;
import cn.com.helei.common.dto.Result;
import cn.com.helei.common.entity.TelegramAccount;
import cn.com.helei.db_layer.mapper.TelegramAccountMapper;
import cn.com.helei.rpc.ITelegramAccountRPC;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-06
 */
@Slf4j
@DubboService
public class TelegramAccountServiceImpl extends AbstractBaseService<TelegramAccountMapper, TelegramAccount> implements ITelegramAccountRPC {


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
}
