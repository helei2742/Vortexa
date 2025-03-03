package cn.com.helei.db_layer.mapper;

import cn.com.helei.common.entity.AccountBaseInfo;
import cn.hutool.core.lang.Pair;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
public interface AccountBaseInfoMapper extends IBaseMapper<AccountBaseInfo> {

    @Select("select type as key, count(*) as value from t_account_base_info group by type")
    List<Pair<String, Integer>> queryTypedInfo();
}
