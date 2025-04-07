package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.bot_platform.service.IBrowserEnvService;
import cn.com.vortexa.rpc.api.platform.IBrowserEnvRPC;
import cn.com.vortexa.common.config.SystemConfig;
import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.excel.ExcelReadUtil;
import cn.com.vortexa.db_layer.service.AbstractBaseService;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.BrowserEnv;
import cn.com.vortexa.common.util.pool.IdMarkPool;
import cn.com.vortexa.bot_platform.mapper.BrowserEnvMapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.com.vortexa.common.entity.BrowserEnv.USER_AGENT_KEY;

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
public class BrowserEnvServiceImpl extends AbstractBaseService<BrowserEnvMapper, BrowserEnv>
        implements IBrowserEnvRPC, IBrowserEnvService {

    private IdMarkPool<BrowserEnv> pool;

    @Override
    public Result saveBrowserEnvs(List<Map<String, Object>> rawLines) {
        if (rawLines == null || rawLines.isEmpty()) {
            return Result.fail("导入数据不能为空");
        }

        try {
            importFromRaw(rawLines);
            return Result.ok();
        } catch (Exception e) {
            log.error("save browser env error", e);
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
            if (userAgent == null) {
                return null;
            }

            BrowserEnv browserEnv = new BrowserEnv();
            browserEnv.setUserAgent((String) userAgent);
            browserEnv.setOtherHeader(map);
            return browserEnv;
        }).filter(Objects::nonNull).toList();

        return insertOrUpdateBatch(list);
    }

    @Override
    public List<BrowserEnv> batchQueryByIdsRPC(List<Serializable> ids) {
        return super.batchQueryByIds(ids);
    }

    @Override
    public BrowserEnv queryByIdRPC(Serializable id) {
        return super.queryById(id);
    }

    @Override
    public PageResult<BrowserEnv> conditionPageQueryRPC(int page, int limit, Map<String, Object> filterMap)
            throws SQLException {
        return super.conditionPageQuery(page, limit, filterMap);
    }
}
