package cn.com.vortexa.db_layer.service;


import cn.com.vortexa.common.dto.PageResult;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface IBaseService<T> {
    Integer insertOrUpdate(T t) throws SQLException;

    Integer insertOrUpdateBatch(List<T> tList) throws SQLException;

    PageResult<T> conditionPageQuery(int page, int limit, String params, Map<String, Object> filterMap) throws SQLException;

    PageResult<T> conditionPageQuery(int page, int limit, Map<String, Object> filterMap) throws SQLException;

    List<T> conditionQuery(Map<String, Object> filterMap) throws SQLException;

    List<T> conditionQuery(String params, Map<String, Object> filterMap) throws SQLException;

    T queryById(Serializable id);

    Boolean delete(List<Integer> ids);
}
