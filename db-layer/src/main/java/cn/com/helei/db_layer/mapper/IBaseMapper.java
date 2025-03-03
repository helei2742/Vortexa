package cn.com.helei.db_layer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

public interface IBaseMapper<T> extends BaseMapper<T> {
    Integer insertOrUpdate(T t);

    List<T> multipleConditionQuery(T query);
}
