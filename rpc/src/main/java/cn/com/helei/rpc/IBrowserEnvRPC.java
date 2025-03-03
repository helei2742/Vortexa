package cn.com.helei.rpc;

import cn.com.helei.common.service.IBaseService;
import cn.com.helei.common.service.ImportService;
import cn.com.helei.common.dto.Result;
import cn.com.helei.common.entity.BrowserEnv;

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
public interface IBrowserEnvRPC extends IBaseService<BrowserEnv>, ImportService {

    Result saveBrowserEnvs(List<Map<String, Object>> rawLines);

    List<BrowserEnv> getUselessBrowserEnv(int count);
}
