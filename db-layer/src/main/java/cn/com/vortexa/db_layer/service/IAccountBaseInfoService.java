package cn.com.vortexa.db_layer.service;

import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.AccountBaseInfo;

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
public interface IAccountBaseInfoService extends IBaseService<AccountBaseInfo>, ImportService {

    Result saveAccountBaseInfos(List<Map<String, Object>> rawLines);

    Result queryTypedInfo();
}
