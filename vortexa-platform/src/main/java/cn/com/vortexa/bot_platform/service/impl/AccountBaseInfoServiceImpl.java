package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.bot_platform.service.IAccountBaseInfoService;
import cn.com.vortexa.rpc.api.platform.IAccountBaseInfoRPC;
import cn.com.vortexa.common.config.SystemConfig;
import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.excel.ExcelReadUtil;
import cn.com.vortexa.bot_platform.mapper.AccountBaseInfoMapper;
import cn.com.vortexa.db_layer.service.AbstractBaseService;
import cn.com.vortexa.common.entity.AccountBaseInfo;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

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
public class AccountBaseInfoServiceImpl extends AbstractBaseService<AccountBaseInfoMapper, AccountBaseInfo>
        implements IAccountBaseInfoRPC, IAccountBaseInfoService {

    public static final String DEFAULT_ACCOUNT_TYPE = "default";

    public AccountBaseInfoServiceImpl() {
        super(accountBaseInfo -> {
            accountBaseInfo.setInsertDatetime(LocalDateTime.now());
            accountBaseInfo.setUpdateDatetime(LocalDateTime.now());
            accountBaseInfo.setIsValid(1);
        });
    }

    @Override
    public List<AccountBaseInfo> batchQueryByIdsRPC(List<Serializable> ids) {
        return super.batchQueryByIds(ids);
    }

    @Override
    public AccountBaseInfo queryByIdRPC(Serializable id) {
        return super.queryById(id);
    }

    @Override
    public PageResult<AccountBaseInfo> conditionPageQueryRPC(int page, int limit, Map<String, Object> filterMap)
            throws SQLException {
        return super.conditionPageQuery(page, limit, filterMap);
    }

    @Override
    public Result saveAccountBaseInfos(List<Map<String, Object>> rawLines) {
        if (rawLines == null || rawLines.isEmpty()) {
            return Result.fail("导入数据不能为空");
        }

        try {
            importFromRaw(rawLines);
            return Result.ok();
        } catch (Exception e) {
            log.error("导入账户数据失败", e.getCause());
            return Result.fail("导入账户数据失败," + e.getMessage());
        }
    }

    @Override
    public Result queryTypedInfo() {
        try {
            List<Pair<String, Integer>> typedInfo = getBaseMapper().queryTypedInfo();
            return Result.ok(typedInfo);
        } catch (Exception e) {
            log.error("查询AccountBaseInfo分类信息失败,", e.getCause());
            return Result.fail(e.getCause().getMessage());
        }
    }

    @Override
    public Integer importFromExcel(String botConfigPath) {
        String proxyFilePath = FileUtil.getConfigDirResourcePath(SystemConfig.CONFIG_DIR_BOT_PATH, botConfigPath);
        AtomicInteger total = new AtomicInteger();
        try {
            ExcelReadUtil.readExcelAsMap(
                    proxyFilePath,
                    (type, map) -> AccountBaseInfo.builder()
                            .type(type)
                            .name(autoCast(map.remove("name")))
                            .email(autoCast(map.remove("email")))
                            .password(autoCast(map.remove("password")))
                            .params(map)
                            .build(),
                    (type, accountBaseInfos) -> CompletableFuture.runAsync(() -> {
                        log.info("[{}] 账号基本信息读取完毕, 共[{}]", type, accountBaseInfos.size());
                        Integer insertCount = null;
                        try {
                            insertCount = insertOrUpdateBatch(accountBaseInfos);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }

                        log.info("[{}] 账号基本信息保存成功, 新增[{}], 共[{}]", type, insertCount, accountBaseInfos.size());

                        total.addAndGet(accountBaseInfos.size());
                    })
            );

        } catch (IOException e) {
            log.error("从文件导入账号基本信息出错", e);
        }

        return total.get();
    }

    @Override
    public Integer importFromRaw(List<Map<String, Object>> rawLines) throws SQLException {
        log.info("导入账户基本信息，共[{}]条", rawLines.size());
        List<AccountBaseInfo> list = rawLines.stream().map(map -> {
            String type = autoCast(map.remove("type"));

            return AccountBaseInfo.builder()
                    .type(StrUtil.isBlank(type) ? DEFAULT_ACCOUNT_TYPE : type)
                    .name(autoCast(map.remove("name")))
                    .email(autoCast(map.remove("email")))
                    .password(autoCast(map.remove("password")))
                    .params(map)
                    .build();
        }).toList();

        return insertOrUpdateBatch(list);
    }
}
