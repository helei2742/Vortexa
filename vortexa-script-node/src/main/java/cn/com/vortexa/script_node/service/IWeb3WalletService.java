package cn.com.vortexa.script_node.service;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * @author com.helei
 * @since 2025/4/24 10:20
 */
public interface IWeb3WalletService {

    BigDecimal erc20BalanceCheck(String rpcUrl, String tokenContractAddress, String address) throws IOException;
}
