package cn.com.vortexa.db_layer.service;

import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.ProxyInfo;

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
public interface IProxyInfoService extends IBaseService<ProxyInfo>, ImportService {

    Result saveProxyInfos(List<Map<String, Object>> rawLines);

}
