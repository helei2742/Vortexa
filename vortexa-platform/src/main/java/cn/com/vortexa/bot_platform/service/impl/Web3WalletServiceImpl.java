package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.db_layer.service.AbstractBaseService;

import cn.com.vortexa.bot_platform.mapper.Web3WalletMapper;
import cn.com.vortexa.bot_platform.service.IWeb3WalletService;
import cn.com.vortexa.common.entity.Web3Wallet;

import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author com.helei
 * @since 2025-04-21
 */
@Service
public class Web3WalletServiceImpl extends AbstractBaseService<Web3WalletMapper, Web3Wallet>
        implements IWeb3WalletService {

    @Override
    public Integer importFromExcel(String fileBotConfigPath) throws SQLException {
        return 0;
    }

    @Override
    public Integer importFromRaw(List<Map<String, Object>> rawLines) throws SQLException {
        return 0;
    }
}
