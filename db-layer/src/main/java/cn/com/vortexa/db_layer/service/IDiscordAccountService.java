package cn.com.vortexa.db_layer.service;

import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.DiscordAccount;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
public interface IDiscordAccountService extends IBaseService<DiscordAccount>, ImportService {

    Result saveDiscordAccounts(List<Map<String, Object>> rawLines);

}
