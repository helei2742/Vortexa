package cn.com.vortexa.script_node.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;

import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.db_layer.util.ConditionQueryUtil;
import cn.com.vortexa.script_node.mapper.RewordInfoMapper;
import cn.com.vortexa.script_node.service.IRewordInfoService;
import cn.com.vortexa.common.entity.RewordInfo;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.HashMap;

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
}
