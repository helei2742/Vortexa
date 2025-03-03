package cn.com.helei.db_layer.service;

import cn.com.helei.common.dto.Result;
import com.github.pagehelper.PageInfo;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface IBaseService<T> {
    Integer insertOrUpdate(T t) throws SQLException;

    Integer insertOrUpdateBatch(List<T> tList) throws SQLException;

    PageInfo<T> conditionPageQuery(int page, int limit, Map<String, Object> filterMap) throws SQLException;

    Result delete(List<Integer> ids);
}
