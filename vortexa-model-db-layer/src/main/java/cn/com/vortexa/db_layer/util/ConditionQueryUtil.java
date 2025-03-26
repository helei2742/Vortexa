package cn.com.vortexa.db_layer.util;

import cn.com.vortexa.common.entity.AccountBaseInfo;
import cn.com.vortexa.common.util.ConditionBuildUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class ConditionQueryUtil {


    public static <T> PageInfo<T> conditionQuery(
            int page,
            int limit,
            Map<String, Object> filterMap,
            String paramsKey,
            Function<T, List<T>> normalQueryFun,
            Class<T> tClass
    ) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        log.info("收到查询消息 [{}]-[{}]-{}", page, limit, filterMap);

        try (Page<AccountBaseInfo> Void = PageHelper.startPage(page, limit)) {
            T condition = ConditionBuildUtil.getMapContainsCondition(
                    filterMap,
                    paramsKey,
                    tClass
            );

            // 执行自定义查询
            List<T> list = normalQueryFun.apply(condition);

            // 包装成 PageInfo 对象，便于返回分页结果
            PageInfo<T> data = new PageInfo<>(list);

            log.info("[{}]-[{}]-{}查询成功, {}条", page, limit, filterMap, list.size());

            return data;
        }
    }
}
