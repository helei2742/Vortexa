package cn.com.vortexa.db_layer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface IBaseMapper<T> extends BaseMapper<T> {
    Integer insertOrUpdate(T t);

    Integer insertOrUpdateBatch(@Param("list") List<T> t);

    List<T> multipleConditionQuery(T query);
}
