package cn.com.vortexa.web3.service;

import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.web3.SignatureMessage;
import cn.com.vortexa.common.entity.Web3Wallet;
import cn.com.vortexa.web3.EthWalletUtil;
import cn.com.vortexa.web3.constants.Web3jFunctionType;
import cn.com.vortexa.web3.dto.SCInvokeParams;
import cn.com.vortexa.web3.dto.SCInvokeResult;
import cn.com.vortexa.web3.dto.Web3ChainInfo;
import cn.com.vortexa.web3.exception.ABIInvokeException;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

/**
 * @author com.helei
 * @since 2025/4/24 16:08
 */
public interface IWeb3WalletOPTRPC {
    /**
     * 对消息签名
     * 必须使用已注册的钱包，使用钱包id标识签名的钱包
     *
     * @param message message
     * @return Result
     */
    Result signatureMessageRPC(SignatureMessage message);

    /**
     * id批量查
     *
     * @param ids ids
     * @return TwitterAccount
     */
    List<Web3Wallet> batchQueryByIdsRPC(List<Serializable> ids);

    /**
     * 调用erc20 abi方法
     *
     * @param scInvokeParams abi调用参数
     * @return 调用结果
     */
    SCInvokeResult erc20ABIInvokeRPC(SCInvokeParams scInvokeParams) throws ABIInvokeException;

    /**
     * 代币授权
     *
     * @param chainInfo     chainInfo
     * @param walletId      钱包id
     * @param tokenContract 合约地址
     * @param amount        授权数量
     * @return Result
     * @throws ABIInvokeException ABIInvokeException
     */
    default String erc20ApproveRPC(Web3ChainInfo chainInfo, Integer walletId, String tokenContract, String spenderAddress, BigInteger amount)
            throws ABIInvokeException {
        SCInvokeResult result = erc20ABIInvokeRPC(SCInvokeParams.builder()
                .walletId(walletId)
                .chainInfo(chainInfo)
                .contractAddress(tokenContract)
                .functionName("approve")
                .paramsTypes(List.of(
                        Pair.of(Web3jFunctionType.Address, spenderAddress),
                        Pair.of(Web3jFunctionType.Uint256, amount)
                ))
                .resultTypes(List.of(Web3jFunctionType.Bool))
                .build()
        );
        return result.getTransactionHash();
    }

    /**
     * 查某token的数量
     *
     * @param chainInfo     chainInfo
     * @param walletId      钱包id
     * @param tokenContract 合约地址
     * @param walletAddress 钱包地址
     * @return Result
     * @throws ABIInvokeException ABIInvokeException
     */
    default BigInteger erc20BalanceOfRPC(Web3ChainInfo chainInfo, Integer walletId, String tokenContract, String walletAddress)
            throws ABIInvokeException {
        SCInvokeResult result = erc20ABIInvokeRPC(SCInvokeParams.builder()
                .walletId(walletId)
                .chainInfo(chainInfo)
                .readFunction(false)
                .functionName("balanceOf")
                .contractAddress(tokenContract)
                .paramsTypes(List.of(
                        Pair.of(Web3jFunctionType.Address, walletAddress)
                ))
                .resultTypes(List.of(Web3jFunctionType.Uint256))
                .build()
        );

        try {
            return (BigInteger) result.getResult().getFirst();
        } catch (Exception e) {
            throw new ABIInvokeException("erc20 balanceOf rpc error, " + (e.getCause() == null ? "" : e.getCause().getMessage()));
        }
    }

    /**
     * 查看spenderAddress能够使用的tokenContract的数量
     *
     * @param chainInfo      chainInfo
     * @param walletId       钱包id
     * @param tokenContract  合约地址
     * @param spenderAddress spenderAddress
     * @return Result
     * @throws ABIInvokeException ABIInvokeException
     */
    default BigInteger erc20AllowanceRPC(Web3ChainInfo chainInfo, Integer walletId, String tokenContract, String spenderAddress, String walletAddress)
            throws ABIInvokeException {
        SCInvokeResult result = erc20ABIInvokeRPC(SCInvokeParams.builder()
                .walletId(walletId)
                .chainInfo(chainInfo)
                .functionName("allowance")
                .contractAddress(tokenContract)
                .paramsTypes(List.of(
                        Pair.of(Web3jFunctionType.Address, walletAddress),
                        Pair.of(Web3jFunctionType.Address, spenderAddress)
                ))
                .resultTypes(List.of(Web3jFunctionType.Uint256))
                .build()
        );

        try {
            return (BigInteger) result.getResult().getFirst();
        } catch (Exception e) {
            throw new ABIInvokeException("erc20 allowance rpc error, " + (e.getCause() == null ? "" : e.getCause().getMessage()));
        }
    }

    /**
     * 查看spenderAddress能够使用的tokenContract的数量
     *
     * @param chainInfo     chainInfo
     * @param walletId      钱包id
     * @param tokenContract 合约地址
     * @return Result
     * @throws ABIInvokeException ABIInvokeException
     */
    default BigInteger erc20DecimalsRPC(Web3ChainInfo chainInfo, Integer walletId, String tokenContract)
            throws ABIInvokeException {
        SCInvokeResult result = erc20ABIInvokeRPC(SCInvokeParams.builder()
                .walletId(walletId)
                .chainInfo(chainInfo)
                .functionName("decimals")
                .readFunction(true)
                .contractAddress(tokenContract)
                .paramsTypes(List.of())
                .resultTypes(List.of(Web3jFunctionType.Uint256))
                .build()
        );

        try {
            return (BigInteger) result.getResult().getFirst();
        } catch (Exception e) {
            throw new ABIInvokeException("erc20 decimals rpc error, " + (e.getCause() == null ? "" : e.getCause().getMessage()));
        }
    }
}
