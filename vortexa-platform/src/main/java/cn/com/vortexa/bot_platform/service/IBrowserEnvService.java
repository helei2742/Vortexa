package cn.com.vortexa.bot_platform.service;

import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.BrowserEnv;
import cn.com.vortexa.db_layer.service.IBaseService;
import cn.com.vortexa.db_layer.service.ImportService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
public interface IBrowserEnvService extends IBaseService<BrowserEnv>, ImportService {

    Result saveBrowserEnvs(List<Map<String, Object>> rawLines);

    List<BrowserEnv> getUselessBrowserEnv(int count);
}
