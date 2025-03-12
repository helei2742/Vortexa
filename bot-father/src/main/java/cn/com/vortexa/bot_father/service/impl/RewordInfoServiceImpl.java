package cn.com.vortexa.bot_father.service.impl;

import cn.com.vortexa.bot_father.mapper.RewordInfoMapper;
import cn.com.vortexa.common.entity.RewordInfo;
import cn.com.vortexa.db_layer.service.AbstractBaseService;
import cn.com.vortexa.rpc.bot.IRewordInfoRPC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@Service
public class RewordInfoServiceImpl extends AbstractBaseService<RewordInfoMapper, RewordInfo> implements IRewordInfoRPC {

    public RewordInfoServiceImpl() {
        super(rewordInfo -> {
            rewordInfo.setInsertDatetime(LocalDateTime.now());
            rewordInfo.setUpdateDatetime(LocalDateTime.now());
            rewordInfo.setIsValid(1);
        });
    }
}
