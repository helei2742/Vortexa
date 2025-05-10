package cn.com.vortexa.bot_platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cn.com.vortexa.bot_platform.entity.BotLaunchConfig;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author com.helei
 * @since 2025-02-07
 */
public interface BotLaunchConfigMapper extends BaseMapper<BotLaunchConfig> {

    int insertOrUpdate(BotLaunchConfig botLaunchConfig);
}
