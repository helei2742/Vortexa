package cn.com.vortexa.bot_platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.com.vortexa.bot_platform.mapper.RewordInfoMapper;
import cn.com.vortexa.common.entity.RewordInfo;
import cn.com.vortexa.rpc.api.platform.IRewordInfoRPC;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RewordInfoServiceImpl extends ServiceImpl<RewordInfoMapper, RewordInfo> implements IRewordInfoRPC {

    @Lazy
    @Autowired
    private RewordInfoServiceImpl rewordInfoService;

    @Override
    public void saveBatchRPC(Integer botId, String botKey, List<RewordInfo> rewordInfos) {
        try {
            // Step 1 检查表是否存在
            getBaseMapper().createIfTableNotExist(botId, botKey);

            // Step 2 保存
            int i = getBaseMapper().saveBatch(botId, botKey, rewordInfos);
        } catch (Exception e) {
            throw new RuntimeException("save [%s-%s] reword error".formatted(botId, botKey), e);
        }
    }
}
