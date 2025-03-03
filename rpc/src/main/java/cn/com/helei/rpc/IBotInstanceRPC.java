package cn.com.helei.rpc;


import cn.com.helei.common.service.IBaseService;
import cn.com.helei.common.entity.BotInstance;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-18
 */
public interface IBotInstanceRPC extends IBaseService<BotInstance> {


    Boolean existsBotInstance(BotInstance query);

}
