package cn.com.helei.rpc;

import cn.com.helei.common.service.IBaseService;
import cn.com.helei.common.service.ImportService;
import cn.com.helei.common.dto.Result;
import cn.com.helei.common.entity.DiscordAccount;

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
public interface IDiscordAccountRPC extends  IBaseService<DiscordAccount>, ImportService {

    Result saveDiscordAccounts(List<Map<String, Object>> rawLines);

}
