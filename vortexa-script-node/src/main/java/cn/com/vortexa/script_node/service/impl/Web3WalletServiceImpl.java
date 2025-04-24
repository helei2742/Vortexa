package cn.com.vortexa.script_node.service.impl;

import cn.com.vortexa.script_node.service.IWeb3WalletService;
import cn.com.vortexa.web3.EthWalletUtil;
import cn.com.vortexa.web3.constants.Web3jFunctionType;
import cn.com.vortexa.web3.util.ABIFunctionBuilder;
import cn.hutool.core.collection.CollUtil;

import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.Type;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author com.helei
 * @since 2025/4/24 10:21
 */
@Service
public class Web3WalletServiceImpl implements IWeb3WalletService {
    @Override
    public BigDecimal erc20BalanceCheck(String rpcUrl, String tokenContractAddress, String address) throws IOException {
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
            return (BigDecimal) result.getFirst().getValue();
        }
    }
}
