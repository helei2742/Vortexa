package cn.com.vortexa.bot_platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cn.com.vortexa.common.entity.ScriptNode;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author com.helei
 * @since 2025-04-08
 */
public interface ScriptNodeMapper extends BaseMapper<ScriptNode> {

    Integer insertOrUpdate(ScriptNode scriptNode);
}
