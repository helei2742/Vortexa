package cn.com.vortexa.script_node.bot.api;


import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.common.entity.Web3Wallet;
import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import cn.com.vortexa.web3.EthWalletUtil;
import cn.com.vortexa.web3.constants.Web3jFunctionType;
import cn.com.vortexa.web3.dto.SCInvokeParams;
import cn.com.vortexa.web3.dto.SCInvokeResult;
import cn.com.vortexa.web3.dto.Web3ChainInfo;
import cn.com.vortexa.web3.exception.ABIInvokeException;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author helei
 * @since 2025-04-26
 */
@Getter
public class ERC20Api {
    private static final Map<String, Integer> rpcUrlDecimalsMap = new ConcurrentHashMap<>();

    private final AutoLaunchBot<?> autoLaunchBot;

    public ERC20Api(AutoLaunchBot<?> autoLaunchBot) {
        this.autoLaunchBot = autoLaunchBot;
    }

    /**
     * 计算erc20 rpcUrl中代币的decimal
     *
     * @param rpcUrl       rpcUrl
     * @param tokenAddress 代币地址
     * @return BigInteger
     * @throws ABIInvokeException ABIInvokeException
     */
    public Integer erc20Decimal(
            String rpcUrl, String tokenAddress
    ) throws ABIInvokeException {
        String key = rpcUrl + "-" + tokenAddress;
        Integer bigInteger = rpcUrlDecimalsMap.get(key);
        if (bigInteger != null) {
            return bigInteger;
        }

        Integer value = autoLaunchBot.getBotApi().getWeb3WalletService().erc20Decimal(
                rpcUrl, tokenAddress
        );
        rpcUrlDecimalsMap.put(key, value);

        if (value != null) {
            return value;
        } else {
            throw new ABIInvokeException("check erc20 allowance fail, result is empty");
        }
    }

    /**
     * 检查账户授权
     *
     * @param accountContext 账户
     * @param rpcUrl         链的rpc url
     * @param tokenAddress   代币地址
     * @param spenderAddress 授权地址
     */
    public BigDecimal erc20Allowance(
            AccountContext accountContext, String rpcUrl, String tokenAddress, String spenderAddress
    ) throws ABIInvokeException {
        Integer decimal = erc20Decimal(rpcUrl, tokenAddress);
        String walletAddress = getACEthAddress(accountContext);

        BigInteger value = autoLaunchBot.getBotApi().getWeb3WalletService().erc20AllowanceCheck(
                rpcUrl, tokenAddress, walletAddress, spenderAddress
        );

        if (value != null) {
            return EthWalletUtil.formatUnits(value, decimal);
        } else {
            throw new ABIInvokeException("check erc20 allowance fail, result is empty");
        }
    }


    /**
     * 授权token
     *
     * @param chainInfo      链信息
     * @param accountContext accountContext
     * @param tokenAddress   代币地址
     * @param spenderAddress 授权地址
     * @param amount         授权数量
     * @return 是否成功
     * @throws ABIInvokeException ABIInvokeException
     */
    public Boolean erc20ApproveToken(
            Web3ChainInfo chainInfo,
            AccountContext accountContext,
            String tokenAddress,
            String spenderAddress,
            BigDecimal amount
    ) throws ABIInvokeException {
        Integer decimal = erc20Decimal(chainInfo.getRpcUrl(), tokenAddress);

        if (accountContext == null || accountContext.getWalletId() == null) {
            throw new IllegalArgumentException("accountContext wallet is null");
        }
        try {
            String transactionHash = autoLaunchBot.getBotApi().getWeb3WalletRPC().erc20ApproveRPC(
                    chainInfo,
                    accountContext.getWalletId(),
                    tokenAddress,
                    spenderAddress,
                    EthWalletUtil.parseUnits(amount, decimal)
            );

            if (StrUtil.isBlank(transactionHash)) {
                return Boolean.FALSE;
            } else {
                TransactionReceipt receipt = EthWalletUtil.waitForTransactionReceipt(chainInfo.getRpcUrl(), transactionHash);
                return EthWalletUtil.isTransactionReceiptSuccess(receipt);
            }
        } catch (Exception e) {
            throw new ABIInvokeException("erc20 approve rpc error", e);
        }
    }

    /**
     * 检查合约允许的代币数量，如果小于amount，则更新
     *
     * @param chainInfo      链信息
     * @param accountContext accountContext
     * @param tokenAddress   代币地址
     * @param spenderAddress 授权地址
     * @param amount         授权数量
     * @return 是否成功
     * @throws ABIInvokeException ABIInvokeException
     */
    public Boolean checkAndApproveToken(
            Web3ChainInfo chainInfo,
            AccountContext accountContext,
            String tokenAddress,
            String spenderAddress,
            BigDecimal amount
    ) throws ABIInvokeException {
        BigDecimal allowance = erc20Allowance(accountContext, chainInfo.getRpcUrl(), tokenAddress, spenderAddress);
        if (allowance == null) {
            throw new ABIInvokeException("check erc20 allowance fail, result is empty");
        }

        if (allowance.compareTo(amount) < 0) {
            return erc20ApproveToken(chainInfo, accountContext, tokenAddress, spenderAddress, amount);
        } else {
            return true;
        }
    }

    /**
     * 代币余额检查
     *
     * @param rpcUrl         rpc地址
     * @param accountContext 账户
     * @param tokenAddress   代币地址
     * @return 余额（已处理完成的）
     * @throws ABIInvokeException ABIInvokeException
     */
    public BigDecimal erc20TokenBalance(
            String rpcUrl, AccountContext accountContext, String tokenAddress
    ) throws ABIInvokeException {
        Integer decimal = erc20Decimal(rpcUrl, tokenAddress);

        String address = getACEthAddress(accountContext);

        BigInteger bigInteger = autoLaunchBot.getBotApi().getWeb3WalletService().erc20BalanceCheck(
                rpcUrl, tokenAddress, address
        );

        return EthWalletUtil.formatUnits(bigInteger, decimal);
    }

    /**
     * 上链的abi调用
     *
     * @param chainInfo       chainInfo
     * @param accountContext  accountContext
     * @param contractAddress contractAddress
     * @param functionName    functionName
     * @param paramsTypes     paramsTypes
     * @param resultTypes     resultTypes
     * @return TransactionReceipt
     * @throws ABIInvokeException ABIInvokeException
     */
    public TransactionReceipt onChainABIInvoke(
            Web3ChainInfo chainInfo,
            AccountContext accountContext,
            String contractAddress,
            String functionName,
            List<Pair<Web3jFunctionType, Object>> paramsTypes,
            List<Web3jFunctionType> resultTypes
    ) throws ABIInvokeException {
        return this.onChainABIInvoke(chainInfo, accountContext, contractAddress, null, functionName, paramsTypes, resultTypes, 3);
    }

    /**
     * 签名abi调用
     *
     * @param chainInfo       chainInfo
     * @param accountContext  accountContext
     * @param contractAddress contractAddress
     * @param value           value
     * @param functionName    functionName
     * @param paramsTypes     paramsTypes
     * @param resultTypes     resultTypes
     * @return TransactionReceipt
     * @throws ABIInvokeException ABIInvokeException
     */
    public TransactionReceipt onChainABIInvoke(
            Web3ChainInfo chainInfo,
            AccountContext accountContext,
            String contractAddress,
            BigDecimal value,
            String functionName,
            List<Pair<Web3jFunctionType, Object>> paramsTypes,
            List<Web3jFunctionType> resultTypes
    ) throws ABIInvokeException {
        return this.onChainABIInvoke(chainInfo, accountContext, contractAddress, value, functionName, paramsTypes, resultTypes, 3);
    }

    /**
     * 签名abi调用
     *
     * @param chainInfo       chainInfo
     * @param accountContext  accountContext
     * @param contractAddress contractAddress
     * @param value           value
     * @param functionName    functionName
     * @param paramsTypes     paramsTypes
     * @param resultTypes     resultTypes
     * @param retryTimes      retryTimes
     * @return TransactionReceipt
     * @throws ABIInvokeException ABIInvokeException
     */
    public TransactionReceipt onChainABIInvoke(
            Web3ChainInfo chainInfo,
            AccountContext accountContext,
            String contractAddress,
            BigDecimal value,
            String functionName,
            List<Pair<Web3jFunctionType, Object>> paramsTypes,
            List<Web3jFunctionType> resultTypes,
            int retryTimes
    ) throws ABIInvokeException {
        Integer walletId = accountContext.getWalletId();
        if (walletId == null) {
            throw new IllegalArgumentException("walletId is null");
        }

        SCInvokeParams.SCInvokeParamsBuilder paramsBuilder = SCInvokeParams
                .builder()
                .chainInfo(chainInfo)
                .walletId(walletId)
                .contractAddress(contractAddress)
                .functionName(functionName)
                .paramsTypes(paramsTypes)
                .resultTypes(resultTypes)
                .retryTimes(retryTimes)
                .readFunction(false);

        return sendAndGetTransactionReceipt(chainInfo, contractAddress, value, paramsBuilder);
    }

    /**
     * 上链的abi调用
     *
     * @param chainInfo       chainInfo
     * @param accountContext  accountContext
     * @param contractAddress contractAddress
     * @param data            data
     * @return TransactionReceipt
     * @throws ABIInvokeException ABIInvokeException
     */
    public TransactionReceipt onChainABIInvoke(
            Web3ChainInfo chainInfo,
            AccountContext accountContext,
            String contractAddress,
            BigDecimal value,
            String data,
            int retryTimes
    ) throws ABIInvokeException {
        Integer walletId = accountContext.getWalletId();
        if (walletId == null) {
            throw new IllegalArgumentException("walletId is null");
        }

        SCInvokeParams.SCInvokeParamsBuilder paramsBuilder = SCInvokeParams
                .builder()
                .chainInfo(chainInfo)
                .walletId(walletId)
                .contractAddress(contractAddress)
                .data(data)
                .retryTimes(retryTimes)
                .readFunction(false);

        return sendAndGetTransactionReceipt(chainInfo, contractAddress, value, paramsBuilder);
    }

    /**
     * 查询token数量，并根据传入百分比获取数量
     *
     * @param accountContext 账户
     * @param tokenAddress   地址
     * @param percent        百分比
     * @return 数量
     * @throws ABIInvokeException ABIInvokeException
     */
    public @NotNull BigDecimal tokenPercentToAmount(String rpcUrl, AccountContext accountContext, String tokenAddress, double percent)
            throws ABIInvokeException {
        BigDecimal balance = erc20TokenBalance(rpcUrl, accountContext, tokenAddress);
        if (balance == null) {
            throw new ABIInvokeException("balance not enough");
        }
        if (percent <= 0 || percent > 1) {
            throw new ABIInvokeException("percent not allowed");
        }

        return balance.multiply(BigDecimal.valueOf(percent));
    }

    @NotNull
    private TransactionReceipt sendAndGetTransactionReceipt(Web3ChainInfo chainInfo, String contractAddress, BigDecimal value, SCInvokeParams.SCInvokeParamsBuilder paramsBuilder) throws ABIInvokeException {
        if (value != null) {
            Integer decimal = erc20Decimal(chainInfo.getRpcUrl(), contractAddress);
            paramsBuilder.value(EthWalletUtil.parseUnits(value, decimal));
        }

        SCInvokeResult scInvokeResult = autoLaunchBot.getBotApi().getWeb3WalletRPC().erc20ABIInvokeRPC(
                paramsBuilder.build()
        );
        String transactionHash = scInvokeResult.getTransactionHash();

        return EthWalletUtil.waitForTransactionReceipt(chainInfo.getRpcUrl(), transactionHash);
    }

    private static @NotNull String getACEthAddress(AccountContext accountContext) {
        Web3Wallet wallet = accountContext.getWallet();
        String walletAddress;
        if (wallet == null || StrUtil.isBlank(walletAddress = wallet.getEthAddress())) {
            throw new IllegalArgumentException("Eth wallet is empty");
        }
        return walletAddress;
    }

}
