package cn.com.vortexa.db_layer.service;

import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.util.ConditionBuildUtil;
import cn.com.vortexa.common.vo.BotInstanceVO;
import cn.com.vortexa.db_layer.mapper.IBaseMapper;
import cn.com.vortexa.db_layer.util.ConditionQueryUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public abstract class AbstractBaseService<M extends IBaseMapper<T>, T> extends ServiceImpl<M, T> implements IBaseService<T> {

    private final Consumer<T> fillFunction;

    protected AbstractBaseService(Consumer<T> fillFunction) {
        this.fillFunction = fillFunction;
    }

    @Override
    public T queryById(Serializable id) {
        return getById(id);
    }

    /**
     * 插入或更新
     *
     * @param t t
     * @return Integer
     */
    @Override
    public Integer insertOrUpdate(T t) throws SQLException {
        fillFunction.accept(t);
        return getBaseMapper().insertOrUpdate(t);
    }


    /**
     * 批量插入或更新
     *
     * @param tList tList
     * @return Integer
     */
    @Override
    public Integer insertOrUpdateBatch(List<T> tList) throws SQLException {
        int successCount = 0;
        for (T t : tList) {
            try {
                Integer count = insertOrUpdate(t);
                successCount += count == null ? 0 : count;
            } catch (Exception e) {
                throw new SQLException("insert or update error", e);
            }
        }

        return successCount;
    }

    /**
     * 条件分页查询
     *
     * @param page page
     * @param limit limit
     * @param filterMap filterMap
     * @return PageInfo<T>
     * @throws SQLException SQLException
     */
    @Override
    public PageResult<T> conditionPageQuery(
            int page,
            int limit,
            Map<String, Object> filterMap
    ) throws SQLException {
        return conditionPageQuery(page, limit, "params", filterMap);
    }


    @Override
    public List<T> batchQueryByIds(List<Serializable> ids) {
        return getBaseMapper().selectBatchIds(ids);
    }

    /**
     * 条件分页查询
     *
     * @param page      page
     * @param limit     limit
     * @param paramsKey paramsKey
     * @param filterMap filterMap
     * @return PageInfo<T>
     * @throws SQLException SQLException
     */
    @Override
    public PageResult<T> conditionPageQuery(
            int page,
            int limit,
            String paramsKey,
            Map<String, Object> filterMap
    ) throws SQLException {
        try {
            PageInfo<T> pageInfo = ConditionQueryUtil.conditionQuery(
                    page,
                    limit,
                    filterMap,
                    paramsKey,
                    condition -> getBaseMapper().multipleConditionQuery(condition),
                    entityClass
            );
            List<T> list = pageInfo.getList();
            PageResult<T> pageResult = new PageResult<>();
            pageResult.setPages(pageInfo.getPages());
            pageResult.setPageNum(pageInfo.getPageNum());
            pageResult.setPageSize(pageInfo.getPageSize());
            pageResult.setTotal(pageInfo.getTotal());
            pageResult.setList(list);
            return pageResult;
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e
        ) {
            throw new SQLException("condition query error", e);
        }
    }

    @Override
    public List<T> conditionQuery(Map<String, Object> filterMap) throws SQLException {
        return conditionQuery("params", filterMap);
    }

    @Override
    public List<T> conditionQuery(String paramsKey, Map<String, Object> filterMap) throws SQLException {
        T condition = null;
        try {
            condition = ConditionBuildUtil.getMapContainsCondition(
                    filterMap,
                    paramsKey,
                    entityClass
            );
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new SQLException(e);
        }

        return getBaseMapper().multipleConditionQuery(condition);
    }

    /**
     * 删除
     *
     * @param ids ids
     * @return Result
     */
    @Transactional
    public Boolean delete(List<Integer> ids) {
        return removeBatchByIds(ids);
    }


    protected <T> T autoCast(Object obj) {
        return obj == null ? null : (T) obj;
    }

    protected Integer toInteger(Object obj) {
        return obj == null ? null : Integer.valueOf(obj.toString());
    }
}
