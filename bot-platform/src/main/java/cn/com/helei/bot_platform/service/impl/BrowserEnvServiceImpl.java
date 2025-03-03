package cn.com.helei.bot_platform.service.impl;

import cn.com.helei.common.config.SystemConfig;
import cn.com.helei.common.util.FileUtil;
import cn.com.helei.common.util.excel.ExcelReadUtil;
import cn.com.helei.db_layer.mapper.IBaseMapper;
import cn.com.helei.db_layer.service.AbstractBaseService;
import cn.com.helei.common.dto.Result;
import cn.com.helei.common.entity.BrowserEnv;
import cn.com.helei.common.util.pool.IdMarkPool;
import cn.com.helei.db_layer.mapper.BrowserEnvMapper;
import cn.com.helei.rpc.IBrowserEnvRPC;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.com.helei.common.entity.BrowserEnv.USER_AGENT_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@Slf4j
@DubboService
public class BrowserEnvServiceImpl extends AbstractBaseService<BrowserEnvMapper, BrowserEnv> implements IBrowserEnvRPC {

    private IdMarkPool<BrowserEnv> pool;

    public BrowserEnvServiceImpl() {
        super(browserEnv -> {
            browserEnv.setInsertDatetime(LocalDateTime.now());
            browserEnv.setUpdateDatetime(LocalDateTime.now());
            browserEnv.setIsValid(1);
        });
    }


    @Override
    public Result saveBrowserEnvs(List<Map<String, Object>> rawLines) {
        if (rawLines == null || rawLines.isEmpty()) {
            return Result.fail("导入数据不能为空");
        }

        try {
            importFromRaw(rawLines);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("导入浏览器环境失败," + e.getMessage());
        }
    }


    @Override
    public synchronized List<BrowserEnv> getUselessBrowserEnv(int count) {
        if (pool == null) {
            log.info("loading Browser Env pool...");
            this.pool = IdMarkPool.create(list(), BrowserEnv.class);
            log.info("loading Browser Env pool success");
        }

        return pool.getLessUsedItem(count);
    }

    @Override
    public Integer importFromExcel(String fileBotConfigPath) throws SQLException {
        String proxyFilePath = FileUtil.getConfigDirResourcePath(SystemConfig.CONFIG_DIR_BOT_PATH, fileBotConfigPath);

        List<Map<String, Object>> headerList = ExcelReadUtil.readExcelToMap(proxyFilePath);

        log.info("文件解析成功, 共[{}]个", headerList.size());
        return importFromRaw(headerList);
    }

    @Override
    public Integer importFromRaw(List<Map<String, Object>> rawLines) throws SQLException {

        List<BrowserEnv> list = rawLines.stream().map(map -> {
            Object userAgent = map.remove(USER_AGENT_KEY);
            if (userAgent == null) return null;

            BrowserEnv browserEnv = new BrowserEnv();
            browserEnv.setUserAgent((String) userAgent);
            browserEnv.setOtherHeader(map);
            return browserEnv;
        }).filter(Objects::nonNull).toList();

        return insertOrUpdateBatch(list);
    }
}
