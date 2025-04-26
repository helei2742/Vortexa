package cn.com.vortexa.script_node.service;

import cn.com.vortexa.web3.exception.ABIInvokeException;

import java.math.BigInteger;

/**
 * @author com.helei
 * @since 2025/4/24 10:20
 */
public interface IWeb3WalletService {


    /**
     * 获取token的decimal
     *
     * @param rpcUrl       rpcUrl
     * @param tokenAddress tokenAddress
     * @return Integer
     * @throws ABIInvokeException ABIInvokeException
     */
    Integer erc20Decimal(String rpcUrl, String tokenAddress) throws ABIInvokeException;

    /**
     * 检查token余额
     *
     * @param rpcUrl       rpcUrl
     * @param tokenAddress tokenAddress
     * @param address      address
     * @return 余额
     * @throws ABIInvokeException ABIInvokeException
     */
    BigInteger erc20BalanceCheck(String rpcUrl, String tokenAddress, String address) throws ABIInvokeException;

    /**
     * 检查token授权金额
     *
     * @param rpcUrl         rpcUrl
     * @param tokenAddress   tokenAddress
     * @param walletAddress  walletAddress
     * @param spenderAddress spenderAddress
     * @return 授权金额
     */
    BigInteger erc20AllowanceCheck(String rpcUrl, String tokenAddress, String walletAddress, String spenderAddress) throws ABIInvokeException;
}
