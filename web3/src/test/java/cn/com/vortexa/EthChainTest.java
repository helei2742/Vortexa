package cn.com.vortexa;

import cn.com.vortexa.web3.constants.EthChainDict;
import cn.com.vortexa.web3.dto.Web3ChainInfo;
import org.junit.Test;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public class EthChainTest {

    private static final String WALLET_ADDRESS = "0xc2e51b0134d65e7e73537c8d08685f3c7f170a57"; // 用户钱包地址

    private static final String ETH_SCAN_API_KEY = "DK9K11Z4EYITYTDQ87WNFU37DR14YC3TPQ";

    @Test
    public void queryMonCount() throws IOException {
        Web3ChainInfo monadChainInfo = EthChainDict.MONAD_TESTNET.getChainInfo();
        Web3j web3j = Web3j.build(new HttpService(monadChainInfo.getRpcUrl()));

        // 获取 Mon 余额
        EthGetBalance ethGetBalance = web3j.ethGetBalance(WALLET_ADDRESS, DefaultBlockParameterName.LATEST).send();
        BigInteger balanceInWei = ethGetBalance.getBalance();

        // ETH 是 18 位小数
        BigDecimal balanceInEth = new BigDecimal(balanceInWei).divide(BigDecimal.TEN.pow(18));

        System.out.println("钱包 " + WALLET_ADDRESS + " 的 Mon 余额: " + balanceInEth + " Mon");
    }


    @Test
    public void transactionHistory() throws IOException {
        Web3j web3j = Web3j.build(new HttpService(EthChainDict.MONAD_TESTNET.getChainInfo().getRpcUrl()));

        BigInteger latestBlock = web3j.ethBlockNumber().send().getBlockNumber();
        System.out.println("最新区块: " + latestBlock);

        // 遍历最近 100 个区块，查找该地址的交易
        for (BigInteger i = latestBlock.subtract(BigInteger.valueOf(100)); i.compareTo(latestBlock) <= 0; i = i.add(BigInteger.ONE)) {
            EthBlock block = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(i), true).send();
            List<EthBlock.TransactionResult> transactions = block.getBlock().getTransactions();
            for (EthBlock.TransactionResult<?> txResult : transactions) {
                EthBlock.TransactionObject tx = (EthBlock.TransactionObject) txResult.get();
                if (WALLET_ADDRESS.equalsIgnoreCase(tx.getFrom()) || WALLET_ADDRESS.equalsIgnoreCase(tx.getTo())) {
                    System.out.println("交易哈希: " + tx.getHash());
                }
            }
        }
    }

    @Test
    public void queryABI() throws IOException {
        String apr_address = "0xb2f82d0f38dc453d596ad40a37799446cc89274a";
        Web3j web3j = Web3j.build(new HttpService(EthChainDict.MONAD_TESTNET.getChainInfo().getRpcUrl()));

        // 获取合约代码
        String contractCode = web3j.ethGetCode(apr_address,DefaultBlockParameterName.LATEST).send().getCode();
        System.out.println("合约代码: " + contractCode);

        if ("0x".equals(contractCode) || contractCode.isEmpty()) {
            System.out.println("该地址上没有智能合约！");
        }
    }

    @Test
    public void contractTest() throws IOException {
        String apr_address = "0xb2f82d0f38dc453d596ad40a37799446cc89274a";

        Web3j web3j = Web3j.build(new HttpService(EthChainDict.MONAD_TESTNET.getChainInfo().getRpcUrl()));

        // 定义合约函数 (假设是一个获取余额的函数 balanceOf)
        Function function = new Function(
                "convertToAssets",
                List.of(new Uint256(2000000)),
                List.of(new TypeReference<Uint256>() {})
        );

        Function balanceQuery = new Function(
                "balanceOf",
                List.of(new Address(WALLET_ADDRESS)),
                List.of(new TypeReference<Uint256>() {})
        );

        // 编码数据
        String encodedFunction = FunctionEncoder.encode(function);

        // 创建交易（使用 Call，不发送交易）
        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(WALLET_ADDRESS, apr_address, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();

        // 解析返回值
        List<Type> result = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        BigInteger assets = (BigInteger) result.getFirst().getValue();


        System.out.println("转换后的资产数：" + assets);

        String encodedFunction2 = FunctionEncoder.encode(balanceQuery);
        // 创建交易（使用 Call，不发送交易）
        EthCall response2 = web3j.ethCall(
                Transaction.createEthCallTransaction(WALLET_ADDRESS, apr_address, encodedFunction2),
                DefaultBlockParameterName.LATEST
        ).send();
        List<Type> result2 = FunctionReturnDecoder.decode(response2.getValue(), balanceQuery.getOutputParameters());
        BigInteger balance = (BigInteger) result2.getFirst().getValue();

        int decimals = 18; // 代币的 decimals，通常是 18
        BigDecimal humanReadableValue = new BigDecimal(balance).divide(BigDecimal.TEN.pow(decimals));


        System.out.println("质押数量： " + humanReadableValue);
    }

    public static void main(String[] args) throws Exception {
//        Web3j web3j = Web3j.build(new HttpService(MONAD_INFURA_URL));
//
//        // 构造 ERC-20 `balanceOf` 方法
//        Function function = new Function(
//                "balanceOf",
//                Collections.singletonList(new org.web3j.abi.datatypes.Address(WALLET_ADDRESS)),
//                Collections.singletonList(new TypeReference<Uint256>() {})
//        );
//
//        String encodedFunction = FunctionEncoder.encode(function);
//        Transaction transaction = Transaction.createEthCallTransaction(WALLET_ADDRESS, MONAD_CONTRACT_ADDRESS, encodedFunction);
//
//        EthCall response = web3j.ethCall(transaction, org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send();
//
//        if (response.hasError()) {
//            System.out.println("Error: " + response.getError().getMessage());
//            return;
//        }
//
//        List<Type> decodedResponse = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
//        BigInteger balance = ((Uint256) decodedResponse.get(0)).getValue();
//
//        System.out.println("代币余额: " + balance);
    }

}
