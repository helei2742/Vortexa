package cn.com.vortexa.bot_platform.web3;

import cn.com.vortexa.web3.EthWalletUtil;
import cn.com.vortexa.web3.constants.Web3jFunctionType;
import cn.com.vortexa.web3.dto.SCInvokeParams;
import cn.com.vortexa.web3.dto.SCInvokeResult;
import cn.com.vortexa.web3.dto.WalletInfo;
import cn.com.vortexa.web3.dto.Web3ChainInfo;
import cn.com.vortexa.web3.exception.ABIInvokeException;
import cn.com.vortexa.web3.util.ABIFunctionBuilder;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Pair;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.EthCall;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能合约调用器
 *
 * @author helei
 * @since 2025/4/23 10:11
 */
public interface SmartContractInvoker {
    /**
     * 执行智能合约交互
     *
     * @param walletInfo     钱包信息
     * @param chainInfo      链信息
     * @param scInvokeParams 调用参数
     * @return SCInvokeResult
     * @throws ABIInvokeException ABIInvokeException
     */
    SCInvokeResult invokeSCFunction(
            WalletInfo walletInfo,
            Web3ChainInfo chainInfo,
            SCInvokeParams scInvokeParams
    ) throws ABIInvokeException;

    enum CHAIN implements SmartContractInvoker {
        ETH {
            @Override
            public SCInvokeResult invokeSCFunction(
                    WalletInfo walletInfo,
                    Web3ChainInfo chainInfo,
                    SCInvokeParams scInvokeParams
            ) throws ABIInvokeException {
                List<Web3jFunctionType> resultTypes = scInvokeParams.getResultTypes();

                // Step 1 构建合约调用方法
                String data = scInvokeParams.getData();
                List<TypeReference<Type>> resultTypeList = new ArrayList<>();
                if (StrUtil.isBlank(data)) {
                    ABIFunctionBuilder functionBuilder = ABIFunctionBuilder
                            .builder()
                            .functionName(scInvokeParams.getFunctionName());

                    if (CollUtil.isNotEmpty(scInvokeParams.getParamsTypes())) {
                        for (Pair<Web3jFunctionType, Object> paramsType : scInvokeParams.getParamsTypes()) {
                            functionBuilder.addParameterType(paramsType.getKey(), paramsType.getValue());
                        }
                    }
                    if (CollUtil.isNotEmpty(resultTypes)) {
                        for (Web3jFunctionType resultType : resultTypes) {
                            functionBuilder.addReturnType(resultType);
                        }
                        resultTypeList.addAll(
                                functionBuilder.getReturnTypes().stream().map(i->(TypeReference<Type>)i).toList()
                        );
                    }
                    data = FunctionEncoder.encode(functionBuilder.build());
                }

                // Step 3 调用合约
                SCInvokeResult result = new SCInvokeResult();
                if (BooleanUtil.isTrue(scInvokeParams.getReadFunction())) {
                    EthCall ethCall = EthWalletUtil.smartContractCallInvoke(
                            chainInfo.getRpcUrl(),
                            scInvokeParams.getContractAddress(),
                            walletInfo.getAddress(),
                            data
                    );
                    // 不上链方法获取值
                    List<Type> types = FunctionReturnDecoder.decode(ethCall.getValue(), resultTypeList);
                    result.setResult(types.stream().map(Type::getValue).toList());
                } else {
                    // 上链方法只获取hash
                    String transactionHash = EthWalletUtil.smartContractTransactionInvoke(
                            chainInfo.getRpcUrl(),
                            scInvokeParams.getContractAddress(),
                            walletInfo.getPrivateKey(),
                            walletInfo.getAddress(),
                            scInvokeParams.getGasLimit(),
                            scInvokeParams.getValue(),
                            data
                    );
                    result.setTransactionHash(transactionHash);
                }

                return result;
            }
        }
    }
}
