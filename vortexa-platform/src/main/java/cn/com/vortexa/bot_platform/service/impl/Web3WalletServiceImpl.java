package cn.com.vortexa.bot_platform.service.impl;

import static cn.com.vortexa.common.entity.Web3Wallet.PUBLIC_FIELDS;

import cn.com.vortexa.bot_platform.web3.SmartContractInvoker;
import cn.com.vortexa.web3.dto.SCInvokeParams;
import cn.com.vortexa.web3.dto.SCInvokeResult;
import cn.com.vortexa.web3.dto.Web3ChainInfo;
import cn.com.vortexa.web3.service.IWeb3WalletOPTRPC;
import cn.hutool.core.collection.CollUtil;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import cn.com.vortexa.bot_platform.mapper.Web3WalletMapper;
import cn.com.vortexa.bot_platform.service.IWeb3WalletService;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.Web3Wallet;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.excel.ExcelReadUtil;
import cn.com.vortexa.db_layer.service.AbstractBaseService;
import cn.com.vortexa.web3.EthWalletUtil;
import cn.com.vortexa.web3.SolanaWalletUtil;
import cn.com.vortexa.common.constants.ChainType;
import cn.com.vortexa.common.dto.web3.SignatureMessage;
import cn.com.vortexa.web3.dto.WalletInfo;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author com.helei
 * @since 2025-04-21
 */
@Slf4j
@Service
public class Web3WalletServiceImpl extends AbstractBaseService<Web3WalletMapper, Web3Wallet>
        implements IWeb3WalletOPTRPC, IWeb3WalletService {

    private final Web3WalletMapper web3WalletMapper;

    public Web3WalletServiceImpl(Web3WalletMapper web3WalletMapper) {
        super();
        this.web3WalletMapper = web3WalletMapper;
    }

    @Override
    public Result signatureMessageRPC(SignatureMessage message) {
        return signatureMessage(message);
    }

    @Override
    public List<Web3Wallet> batchQueryByIdsRPC(List<Serializable> ids) {
        return batchQueryByIds(ids);
    }

    @Override
    public SCInvokeResult erc20ABIInvokeRPC(SCInvokeParams scInvokeParams) throws IOException {
        Integer retryTimes = scInvokeParams.getRetryTimes();
        for (int i = 1; i <= retryTimes; i++) {
            Result result = smartContractInvoke(scInvokeParams);
            if (result.getSuccess()) {
                return (SCInvokeResult) result.getData();
            } else if (i != retryTimes) {
                log.warn("erc20 abi rpc error, start retry[{}/{}]", i, retryTimes);
            }
        }
        throw new RuntimeException("erc20 abi rpc error, retry out of limit:" + retryTimes);
    }

    @Override
    public List<Web3Wallet> batchQueryByIds(List<Serializable> ids) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        QueryWrapper<Web3Wallet> queryWrapper = new QueryWrapper<>();
        queryWrapper.select(PUBLIC_FIELDS);
        queryWrapper.in("id", ids);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public Result saveWallet(List<Map<String, Object>> rawLines) {
        try {
            return Result.ok(importFromRaw(rawLines));
        } catch (SQLException e) {
            log.error("import wallet error", e);
            return Result.fail(e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
        }
    }

    @Override
    public Result signatureMessage(SignatureMessage signatureMessage) {
        Integer walletId;
        String message;
        if ((walletId = signatureMessage.getWalletId()) == null || walletId < 0 || StrUtil.isBlank(
                message = signatureMessage.getMessage())) {
            return Result.fail("params illegal");
        }

        Web3Wallet web3Wallet = baseMapper.selectOne(new QueryWrapper<>(Web3Wallet.builder().id(walletId).build()));
        if (web3Wallet == null) {
            return Result.fail("wallet %s not exist".formatted(walletId));
        }

        ChainType chainType = signatureMessage.getChainType();
        try {
            String signature = switch (chainType) {
                case ETH -> EthWalletUtil.signatureMessage2String(web3Wallet.getEthPrivateKey(), message);
                case SOL -> SolanaWalletUtil.signatureMessage2String(web3Wallet.getSolPrivateKey(), message);
                case null -> throw new IllegalArgumentException("chain type[%s] not support".formatted(chainType));
            };
            return Result.ok(signature);
        } catch (Exception e) {
            log.error("{} signature chain[{}] message[{}] fail", walletId, chainType, message, e);
            return Result.fail("signature failed");
        }
    }

    @Override
    public Result smartContractInvoke(SCInvokeParams invokeParams) throws IOException {
        if (invokeParams.getWalletId() == null && invokeParams.getWalletInfo() == null) {
            return Result.fail("walletId or walletInfo must be provided");
        }

        Web3ChainInfo chainInfo = invokeParams.getChainInfo();
        if (chainInfo == null) {
            return Result.fail("chainInfo must be provided");
        }

        ChainType chainType = chainInfo.getChainType() == null ? ChainType.ETH : chainInfo.getChainType();
        WalletInfo wallet = invokeParams.getWalletInfo() == null
                ? new WalletInfo(chainType, web3WalletMapper.selectById(invokeParams.getWalletId()))
                : invokeParams.getWalletInfo();

        SCInvokeResult result = switch (chainType) {
            case ETH -> SmartContractInvoker.CHAIN.ETH.invokeSCFunction(wallet, chainInfo, invokeParams);
            default ->
                    throw new IllegalArgumentException("chain type[%s] not support".formatted(chainType));
        };

        return Result.ok(result);
    }

    @Override
    public Integer importFromExcel(String fileBotConfigPath) throws SQLException {
        String proxyFilePath = FileUtil.getAppResourceSystemConfigDir() + File.separator + fileBotConfigPath;
        try {
            List<Map<String, Object>> rawLines = ExcelReadUtil.readExcelToMap(proxyFilePath);
            return importFromRaw(rawLines);
        } catch (Exception e) {
            log.error("read wallet file[{}] error", proxyFilePath, e);
            return 0;
        }
    }

    @Override
    public Integer importFromRaw(List<Map<String, Object>> rawLines) throws SQLException {
        List<Web3Wallet> web3Wallets = new ArrayList<>();
        for (Map<String, Object> line : rawLines) {
            String mnemonic = String.valueOf(line.get("mnemonic"));
            Web3Wallet.Web3WalletBuilder builder = Web3Wallet.builder().mnemonic(mnemonic);

            WalletInfo ethWallet = EthWalletUtil.generateWalletInfoFromMnemonic(mnemonic);
            builder.ethPrivateKey(ethWallet.getPrivateKey());
            builder.ethAddress(ethWallet.getAddress());

            WalletInfo solWallet = SolanaWalletUtil.generateWalletInfoFromMnemonic(mnemonic);
            builder.solPrivateKey(solWallet.getPrivateKey());
            builder.solAddress(solWallet.getAddress());

            web3Wallets.add(builder.build());
        }
        if (web3Wallets.isEmpty()) {
            return 0;
        }
        return insertOrUpdateBatch(web3Wallets);
    }
}
