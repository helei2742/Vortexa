package cn.com.vortexa.script_node.service.impl;

import cn.com.vortexa.script_node.service.IWeb3WalletService;
import cn.com.vortexa.web3.EthWalletUtil;
import cn.com.vortexa.web3.constants.Web3jFunctionType;
import cn.com.vortexa.web3.exception.ABIInvokeException;
import cn.com.vortexa.web3.util.ABIFunctionBuilder;
import cn.hutool.core.collection.CollUtil;

import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.Type;

import java.math.BigInteger;
import java.util.List;

/**
 * @author com.helei
 * @since 2025/4/24 10:21
 */
@Service
public class Web3WalletServiceImpl implements IWeb3WalletService {

    @Override
    public Integer erc20Decimal(String rpcUrl, String tokenAddress) throws ABIInvokeException {
        List<Type> result = EthWalletUtil.smartContractCallInvoke(
                rpcUrl,
                tokenAddress,
                null,
                ABIFunctionBuilder.builder()
                        .functionName("decimals")
                        .addReturnType(Web3jFunctionType.Uint256)
        );

        if (!result.isEmpty()) {
            BigInteger value = (BigInteger) result.getFirst().getValue();

            return value.intValue();
        } else {
            throw new ABIInvokeException("check erc20 decimals fail, result is empty");
        }
    }

    @Override
    public BigInteger erc20BalanceCheck(String rpcUrl, String tokenContractAddress, String address) throws ABIInvokeException {
        List<Type> result = EthWalletUtil.smartContractCallInvoke(
                rpcUrl,
                tokenContractAddress,
                address,
                ABIFunctionBuilder
                        .builder()
                        .functionName("balanceOf")
                        .addParameterType(Web3jFunctionType.Address, address)
                        .addReturnType(Web3jFunctionType.Uint256)
        );

        if (CollUtil.isEmpty(result)) {
            throw new RuntimeException("erc20 balance check error");
        } else {
            return (BigInteger) result.getFirst().getValue();
        }
    }

    @Override
    public BigInteger erc20AllowanceCheck(String rpcUrl, String tokenAddress, String walletAddress, String spenderAddress) throws ABIInvokeException {
        List<Type> result = EthWalletUtil.smartContractCallInvoke(
                rpcUrl,
                tokenAddress,
                walletAddress,
                ABIFunctionBuilder.builder()
                        .functionName("allowance")
                        .addParameterType(Web3jFunctionType.Address, walletAddress)
                        .addParameterType(Web3jFunctionType.Address, spenderAddress)
                        .addReturnType(Web3jFunctionType.Uint256)
        );

        if (CollUtil.isEmpty(result)) {
            throw new RuntimeException("erc20 allowance check error");
        } else {
            return (BigInteger) result.getFirst().getValue();
        }
    }
}
