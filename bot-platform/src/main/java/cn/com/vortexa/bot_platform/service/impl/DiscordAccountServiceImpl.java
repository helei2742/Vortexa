package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.common.config.SystemConfig;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.excel.ExcelReadUtil;
import cn.com.vortexa.db_layer.service.AbstractBaseService;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.DiscordAccount;
import cn.com.vortexa.db_layer.mapper.DiscordAccountMapper;
import cn.com.vortexa.rpc.IDiscordAccountRPC;
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
public class DiscordAccountServiceImpl extends AbstractBaseService<DiscordAccountMapper, DiscordAccount> implements IDiscordAccountRPC {

    public DiscordAccountServiceImpl() {
        super(discordAccount -> {
            discordAccount.setInsertDatetime(LocalDateTime.now());
            discordAccount.setUpdateDatetime(LocalDateTime.now());
            discordAccount.setIsValid(1);
        });
    }


    public Result saveDiscordAccounts(List<Map<String, Object>> rawLines) {
        if (rawLines == null || rawLines.isEmpty()) {
            return Result.fail("导入数据不能为空");
        }

        try {
            importFromRaw(rawLines);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("导入discord账号失败," + e.getMessage());
        }
    }

    @Override
    public Integer importFromExcel(String fileBotConfigPath) {
        String dirResourcePath = FileUtil.getConfigDirResourcePath(SystemConfig.CONFIG_DIR_BOT_PATH, fileBotConfigPath);

        try {
            List<Map<String, Object>> rawLines = ExcelReadUtil.readExcelToMap(dirResourcePath);

            return importFromRaw(rawLines);
        } catch (Exception e) {
            log.error("读取discord account 文件[{}]发生异常", dirResourcePath, e);
            return 0;
        }
    }

    @Override
    public Integer importFromRaw(List<Map<String, Object>> rawLines) throws SQLException {
        List<DiscordAccount> discordAccounts = rawLines.stream().map(map -> DiscordAccount.builder()
                .username(autoCast(map.remove("username")))
                .password(autoCast(map.remove("password")))
                .bindEmail(autoCast(map.remove("bind_email")))
                .bindEmailPassword(autoCast(map.remove("bind_email_password")))
                .token(autoCast(map.remove("token")))
                .params(map)
                .build()
        ).toList();

        return insertOrUpdateBatch(discordAccounts);
    }
}
