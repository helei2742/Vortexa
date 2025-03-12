package cn.com.vortexa.db_layer.service;

import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.TelegramAccount;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-06
 */
public interface ITelegramAccountService extends IService<TelegramAccount>, IBaseService<TelegramAccount>, ImportService {


    Result saveTelegrams(List<Map<String, Object>> rawLines);

}
