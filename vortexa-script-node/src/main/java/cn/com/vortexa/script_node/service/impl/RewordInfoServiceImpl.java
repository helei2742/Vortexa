package cn.com.vortexa.script_node.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;

import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.control.anno.RPCReference;
import cn.com.vortexa.db_layer.util.ConditionQueryUtil;
import cn.com.vortexa.rpc.api.platform.IRewordInfoRPC;
import cn.com.vortexa.script_node.mapper.RewordInfoMapper;
import cn.com.vortexa.script_node.service.IRewordInfoService;
import cn.com.vortexa.common.entity.RewordInfo;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@Slf4j
@Service
public class RewordInfoServiceImpl extends ServiceImpl<RewordInfoMapper, RewordInfo> implements IRewordInfoService {

    @Lazy
    @Autowired
    private RewordInfoServiceImpl rewordInfoService;

    @RPCReference
    private IRewordInfoRPC rewordInfoRPC;

    @Override
    public boolean checkAndCreateShardedTable(Integer botId, String botKey) throws SQLException {
        if (botId == null || StrUtil.isBlank(botKey)) {
            log.error("botId/botKey is empty");
            return false;
        }
        try {
            getBaseMapper().createIfTableNotExist(botId, botKey);
            return true;
        } catch (Exception e) {
            throw new SQLException("check and create sharded table[%s]-[%s] error".formatted(botId, botKey), e);
        }
    }

    @Override
    public PageResult<RewordInfo> queryAccountReword(
            Integer pageNum, Integer pageSize, HashMap<String, Object> params
    ) {
        try {
            PageInfo<RewordInfo> pageInfo = ConditionQueryUtil.conditionQuery(pageNum, pageSize, params, null,
                    t -> list(new QueryWrapper<>(t)),
                    RewordInfo.class
            );

            return PageResult.<RewordInfo>builder()
                    .pageNum(pageInfo.getPageNum())
                    .pageSize(pageSize)
                    .pages(pageInfo.getPages())
                    .total(pageInfo.getTotal())
                    .list(pageInfo.getList())
                    .build();
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveAndUploadRewordInfos(Integer botId, String botKey, List<RewordInfo> rewordInfos) {
        if (CollUtil.isNotEmpty(rewordInfos)) {
            // Step 1 保存本地
            rewordInfoService.saveBatch(rewordInfos);
            rewordInfos.forEach(r->r.setId(null));
            // Step 2 上传
            rewordInfoRPC.saveBatchRPC(botId, botKey, rewordInfos);
        }
    }
}
