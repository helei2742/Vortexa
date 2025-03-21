package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.db_layer.service.IProxyInfoService;
import cn.com.vortexa.rpc.IProxyInfoRPC;
import cn.com.vortexa.common.config.SystemConfig;
import cn.com.vortexa.common.constants.ProxyProtocol;
import cn.com.vortexa.common.constants.ProxyType;
import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.excel.ExcelReadUtil;
import cn.com.vortexa.db_layer.service.AbstractBaseService;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.ProxyInfo;
import cn.com.vortexa.db_layer.mapper.ProxyInfoMapper;
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
public class ProxyInfoServiceImpl extends AbstractBaseService<ProxyInfoMapper, ProxyInfo> implements IProxyInfoRPC, IProxyInfoService {


    public ProxyInfoServiceImpl() {
        super(proxyInfo -> {
            proxyInfo.setInsertDatetime(LocalDateTime.now());
            proxyInfo.setUpdateDatetime(LocalDateTime.now());
            proxyInfo.setIsValid(1);
        });

    }


    @Override
    public Result saveProxyInfos(List<Map<String, Object>> rawLines) {
        if (rawLines == null || rawLines.isEmpty()) {
            return Result.fail("导入数据不能为空");
        }

        try {
            importFromRaw(rawLines);
            return Result.ok();
        } catch (Exception e) {
            log.error("saveProxyInfos error", e);
            return Result.fail("导入代理信息失败," + e.getMessage());
        }
    }

    @Override
    public Integer importFromExcel(String botConfigPath) {
        String proxyFilePath = FileUtil.getConfigDirResourcePath(SystemConfig.CONFIG_DIR_BOT_PATH, botConfigPath);

        int total = 0;

        try {
            List<Map<String, Object>> staticProxies = ExcelReadUtil.readExcelToMap(proxyFilePath, "static");

            total += importFromRaw(staticProxies);

            List<Map<String, Object>> dynamicProxies = ExcelReadUtil.readExcelToMap(proxyFilePath, "dynamic");

            total += importFromRaw(dynamicProxies);

            log.info("代理配置文件解析成功，static-proxy:[{}], dynamic-proxy:[{}]", staticProxies.size(), dynamicProxies.size());

            return total;
        } catch (Exception e) {
            log.error("解析代理配置文件[{}]错误", proxyFilePath, e);

            return 0;
        }
    }

    @Override
    public Integer importFromRaw(List<Map<String, Object>> rawLines) throws SQLException {

        List<ProxyInfo> list = rawLines.stream().map(map -> {
            String proxyProtocol = autoCast(map.remove("proxy_protocol"));
            if (proxyProtocol == null) proxyProtocol = autoCast(map.remove("proxyProtocol"));

            Object port = map.remove("port");
            return ProxyInfo.builder()
                    .proxyType(ProxyType.STATIC)
                    .host(autoCast(map.remove("host")))
                    .port(port instanceof Integer ? (Integer) port : Integer.parseInt(autoCast(port)))
                    .proxyProtocol(ProxyProtocol.valueOf(proxyProtocol.toUpperCase()))
                    .username(autoCast(map.remove("username")))
                    .password(autoCast(map.remove("password")))
                    .params(map)
                    .build();
        }).toList();

        return insertOrUpdateBatch(list);
    }

    @Override
    public ProxyInfo queryByIdRPC(Serializable id) {
        return super.queryById(id);
    }

    @Override
    public PageResult<ProxyInfo> conditionPageQueryRPC(int page, int limit, Map<String, Object> filterMap)
            throws SQLException {
        return super.conditionPageQuery(page, limit, filterMap);
    }
}
