package cn.com.vortexa.web3;

import cn.com.vortexa.web3.dto.WalletInfo;
import cn.com.vortexa.web3.exception.ABIInvokeException;
import cn.com.vortexa.web3.util.ABIFunctionBuilder;

import org.bitcoinj.crypto.*;
import org.jetbrains.annotations.NotNull;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author helei
 * @since 2025/3/28 11:16
 */
public class EthWalletUtil {

    public static final ConcurrentMap<String, Web3j> rpcUrlWeb3JMap = new ConcurrentHashMap<>();

    /**
     * 重试次数
     */
    public static final int TRANSACTION_ATTEMPTS = 40;

    /**
     * 间隔
     */
    public static final int TRANSACTION_SLEEP_MILLIS = 1500;

    /**
     * 默认Gas limit
     */
    private static final BigInteger DEFAULT_GAS_LIMIT = BigInteger.valueOf(200_000);

    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * 获取web3j
     *
     * @param rpcUrl rpcUrl
     * @return Web3j
     */
    public static Web3j getRpcWeb3j(String rpcUrl) {
        return rpcUrlWeb3JMap.compute(rpcUrl, (k, v) -> {
            if (v == null) {
                v = Web3j.build(new HttpService(rpcUrl));
            }
            return v;
        });
    }

    /**
     * 生成eth钱包信息
     *
     * @return WalletInfo
     */
    public static WalletInfo generateEthWallet() {
        return generateWalletInfoFromMnemonic(generateMnemonic());
    }

    /**
     * 生成助记词
     *
     * @return String
     */
    public static String generateMnemonic() {
        // 1. 生成助记词
        SecureRandom secureRandom = new SecureRandom();
        byte[] entropy = new byte[16]; // 128 bits entropy
        secureRandom.nextBytes(entropy);

        // 使用 bitcoinj 生成助记词
        var mnemonicCode = MnemonicCode.INSTANCE;
        List<String> mnemonicWords = null;
        try {
            mnemonicWords = mnemonicCode.toMnemonic(entropy);
        } catch (MnemonicException.MnemonicLengthException e) {
            throw new RuntimeException(e);
        }
        return String.join(" ", mnemonicWords);
    }

    /**
     * 助记词生成钱包信息
     *
     * @param mnemonic mnemonic
     * @return WalletInfo
     */
    public static WalletInfo generateWalletInfoFromMnemonic(String mnemonic) {
        // 2. 生成 seed
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, ""); // passphrase 可设置

        // 3. 创建 HD 钱包根节点 (BIP32 Root Key)
        DeterministicKey rootPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed);

        // 4. 分步派生 BIP44 路径 m/44'/60'/0'/0/0
        DeterministicKey purposeKey = HDKeyDerivation.deriveChildKey(rootPrivateKey, new ChildNumber(44, true));
        DeterministicKey coinTypeKey = HDKeyDerivation.deriveChildKey(purposeKey, new ChildNumber(60, true));
        DeterministicKey accountKey = HDKeyDerivation.deriveChildKey(coinTypeKey, new ChildNumber(0, true));
        DeterministicKey externalKey = HDKeyDerivation.deriveChildKey(accountKey, ChildNumber.ZERO); // change = 0
        DeterministicKey addressKey = HDKeyDerivation.deriveChildKey(externalKey,
                ChildNumber.ZERO); // address index = 0

        byte[] privateKeyBytes = addressKey.getPrivKeyBytes();

        // 转为 web3j 的 ECKeyPair
        ECKeyPair keyPair = ECKeyPair.create(privateKeyBytes);
        Credentials credentials = Credentials.create(keyPair);

        // 6. 输出钱包信息
        return WalletInfo.builder()
                .mnemonic(mnemonic)
                .privateKey(keyPair.getPrivateKey().toString(16))
                .publicKey(keyPair.getPublicKey().toString(16))
                .address(credentials.getAddress())
                .build();
    }

    /**
     * 前面消息
     *
     * @param privateKey privateKey
     * @param message    message
     * @return Sign.SignatureData
     */
    public static Sign.SignatureData signatureMessage2Data(String privateKey, String message) {
        byte[] contentHashBytes = message.getBytes();
        // 根据私钥获取凭证对象
        Credentials credentials = Credentials.create(privateKey);
        return Sign.signPrefixedMessage(contentHashBytes, credentials.getEcKeyPair());
    }

    /**
     * 前签名消息成String
     *
     * @param privateKey privateKey
     * @param message    message
     * @return String
     */
    public static String signatureMessage2String(String privateKey, String message) {
        Sign.SignatureData signMessage = signatureMessage2Data(privateKey, message);

        byte[] r = signMessage.getR();
        byte[] s = signMessage.getS();
        byte[] v = signMessage.getV();

        byte[] signByte = Arrays.copyOf(r, v.length + r.length + s.length);
        System.arraycopy(s, 0, signByte, r.length, s.length);
        System.arraycopy(v, 0, signByte, r.length + s.length, v.length);

        return Numeric.toHexString(signByte);
    }

    /**
     * 私钥获取地址
     *
     * @param privateKeyHex privateKeyHex
     * @return String
     */
    public static String getETHAddress(String privateKeyHex) {
        Credentials credentials = Credentials.create(privateKeyHex);

        // 获取原始地址（小写）
        String rawAddress = credentials.getAddress();

        // 将地址转换为区分大小写的 Checksum 地址
        return Keys.toChecksumAddress(rawAddress);
    }


    public static String getRandomNonce() {
        // 生成 32 字节的随机数
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        // 将字节数组转换为十六进制字符串
        return toHex(randomBytes);
    }

    // 将字节数组转换为十六进制字符串
    public static String toHex(byte[] bytes) {
        return Numeric.toHexString(bytes);
    }

    /**
     * 智能合约调用 只读
     *
     * @param rpcUrl          rpcUrl
     * @param contractAddress contractAddress
     * @param address         address
     * @return EthCall EthCall
     * @throws ABIInvokeException ABIInvokeException
     */
    public static EthCall smartContractCallInvoke(
            String rpcUrl,
            String contractAddress,
            String address,
            String data
    ) throws ABIInvokeException {
        Web3j web3j = getRpcWeb3j(rpcUrl);
        Transaction transaction = Transaction.createEthCallTransaction(
                address,
                contractAddress,
                data
        );

        try {
           return web3j.ethCall(
                    transaction,
                    DefaultBlockParameterName.LATEST
            ).send();
        } catch (Exception e) {
            throw new ABIInvokeException(e);
        }
    }
    /**
     * 智能合约调用 只读
     *
     * @param rpcUrl          rpcUrl
     * @param contractAddress contractAddress
     * @param address         address
     * @param functionBuilder functionBuilder
     * @return List<Type>
     * @throws ABIInvokeException ABIInvokeException
     */
    public static List<Type> smartContractCallInvoke(
            String rpcUrl,
            String contractAddress,
            String address,
            ABIFunctionBuilder functionBuilder
    ) throws ABIInvokeException {
        Web3j web3j = getRpcWeb3j(rpcUrl);

        Function function = functionBuilder.build();
        String encode = FunctionEncoder.encode(function);
        Transaction transaction = Transaction.createEthCallTransaction(
                address,
                contractAddress,
                encode
        );

        EthCall response;
        try {
            response = web3j.ethCall(
                    transaction,
                    DefaultBlockParameterName.LATEST
            ).send();

            return FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        } catch (Exception e) {
            throw new ABIInvokeException(e);
        }
    }

    /**
     * 智能合约调用 上链
     *
     * @param rpcUrl          rpcUrl
     * @param contractAddress 合约地址
     * @param primaryKey      钱包私钥
     * @param address         钱包地址
     * @param gasLimit        gasLimit
     * @param value           发生金额
     * @param functionBuilder functionBuilder
     * @return transaction hash
     * @throws ABIInvokeException 网络不通， 获取hash失败都会抛出次异常
     */
    public static String smartContractTransactionInvoke(
            String rpcUrl,
            String contractAddress,
            String primaryKey,
            String address,
            BigInteger gasLimit,
            BigInteger value,
            ABIFunctionBuilder functionBuilder
    ) throws ABIInvokeException {
        Function function = functionBuilder.build();
        return smartContractTransactionInvoke(rpcUrl, contractAddress, primaryKey, address, gasLimit, value, FunctionEncoder.encode(function));
    }

    /**
     * 智能合约调用 上链
     *
     * @param rpcUrl          rpcUrl
     * @param contractAddress 合约地址
     * @param primaryKey      钱包私钥
     * @param address         钱包地址
     * @param gasLimit        gasLimit
     * @param value           发生金额
     * @param data            data
     * @return transaction hash
     * @throws ABIInvokeException 网络不通， 获取hash失败都会抛出次异常
     */
    public static String smartContractTransactionInvoke(
            String rpcUrl,
            String contractAddress,
            String primaryKey,
            String address,
            BigInteger gasLimit,
            BigInteger value,
            String data
    ) throws ABIInvokeException {
        try {
            Web3j web3j = getRpcWeb3j(rpcUrl);

            // Step 1 参数处理
            BigInteger nonce = getNonce(web3j, address);
            BigInteger gasPrice = getGasPrice(web3j);
            if (gasLimit == null) {
                gasLimit = dynamicCalGasLimit(
                        web3j, contractAddress, address, gasPrice, data
                );
            }
            if (value == null) value = BigInteger.ZERO;

            // Step 2 构建交易信息
            RawTransaction rawTX = RawTransaction.createTransaction(
                    nonce, gasPrice, gasLimit, contractAddress, value, data
            );

            byte[] signedMessage = TransactionEncoder.signMessage(rawTX, Credentials.create(primaryKey));
            String hexString = Numeric.toHexString(signedMessage);

            // Step 3 发生交友
            EthSendTransaction send = web3j.ethSendRawTransaction(hexString).send();

            // Step 4 获取交易hash值
            if (send.hasError()) {
                throw new ABIInvokeException("contract transaction Error: " + send.getError().getMessage());
            }
            return send.getTransactionHash();
        } catch (Exception e) {
            throw new ABIInvokeException("contract transaction Error: ", e);
        }
    }

    /**
     * 动态计算gasLimit
     *
     * @return BigInteger
     */
    public static BigInteger dynamicCalGasLimit(
            Web3j web3j,
            String contractAddress,
            String walletAddress,
            BigInteger gasPrice,
            String encodeFunction
    ) throws ABIInvokeException {
        try {
            EthEstimateGas ethEstimateGas = web3j.ethEstimateGas(Transaction.createFunctionCallTransaction(
                    walletAddress,
                    null,
                    gasPrice,
                    null,
                    contractAddress,
                    BigInteger.ZERO,
                    encodeFunction
            )).send();

            if (ethEstimateGas.hasError()) {
                throw new ABIInvokeException("dynamic cal gas failed: " + ethEstimateGas.getError().getMessage());
            }
            return ethEstimateGas.getAmountUsed();
        } catch (Exception e) {
            throw new ABIInvokeException("dynamic cal gas fee error", e);
        }
    }

    /**
     * 等待交易完成
     *
     * @param rpcUrl          rpcUrl
     * @param transactionHash transactionHash
     * @return TransactionReceipt
     * @throws ABIInvokeException ABIInvokeException
     */
    public static TransactionReceipt waitForTransactionReceipt(String rpcUrl, String transactionHash) throws ABIInvokeException {
        return waitForTransactionReceipt(getRpcWeb3j(rpcUrl), transactionHash);
    }

    /**
     * 等待交易完成
     *
     * @param web3j           web3j
     * @param transactionHash 交易hash
     * @return TransactionReceipt
     * @throws ABIInvokeException 等待完成
     */
    public static TransactionReceipt waitForTransactionReceipt(
            Web3j web3j, String transactionHash
    ) throws ABIInvokeException {
        for (int i = 0; i < TRANSACTION_ATTEMPTS; i++) {
            try {
                EthGetTransactionReceipt transactionReceipt = web3j.ethGetTransactionReceipt(transactionHash).send();
                if (transactionReceipt.getTransactionReceipt().isPresent()) {
                    return transactionReceipt.getTransactionReceipt().get();
                } else {
                    try {
                        Thread.sleep(TRANSACTION_SLEEP_MILLIS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        throw new ABIInvokeException("Transaction receipt not received after "
                + (TRANSACTION_ATTEMPTS * TRANSACTION_SLEEP_MILLIS / 1000) + " seconds");
    }

    /**
     * 获取地址nonce
     *
     * @param rpcUrl  rpcUrl
     * @param address address
     * @return nonce
     * @throws IOException IOException
     */
    public static BigInteger getNonce(String rpcUrl, String address) throws IOException {
        return getNonce(getRpcWeb3j(rpcUrl), address);
    }

    /**
     * 获取地址nonce
     *
     * @param web3j   web3j
     * @param address address
     * @return nonce
     * @throws IOException IOException
     */
    public static BigInteger getNonce(@NotNull Web3j web3j, String address) throws IOException {
        return web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING)
                .send().getTransactionCount();
    }

    /**
     * 获取rpc的gas
     *
     * @param rpcUrl rpcUrl
     * @return nonce
     * @throws IOException IOException
     */
    public static BigInteger getGasPrice(String rpcUrl) throws IOException {
        return getGasPrice(getRpcWeb3j(rpcUrl));
    }

    /**
     * 获取rpc的gas
     *
     * @param web3j web3j
     * @return nonce
     * @throws IOException IOException
     */
    public static BigInteger getGasPrice(@NotNull Web3j web3j) throws IOException {
        return web3j.ethGasPrice().send().getGasPrice();
    }

    /**
     * 数量单位化，乘decimals
     *
     * @param amount   数量
     * @param decimals 多少个0
     * @return 单位化的数量
     */
    public static BigInteger parseUnits(BigDecimal amount, int decimals) {
        return amount.multiply(BigDecimal.TEN.pow(decimals))
                .setScale(0, RoundingMode.DOWN).toBigIntegerExact();
    }

    /**
     * 数量单位化，除decimals
     *
     * @param amount   数量
     * @param decimals 多少个0
     * @return 单位化的数量
     */
    public static BigDecimal formatUnits(BigInteger amount, int decimals) {
        return new BigDecimal(amount).divide(BigDecimal.TEN.pow(decimals), 2, RoundingMode.DOWN);
    }

    /**
     * 根据滑点百分比计算最小可接受输出（整数版本）
     *
     * @param amountInWei     预期数量（最小单位，比如 wei）
     * @param slippagePercent 滑点百分比（例如 1 表示 1%）
     * @return 最小可接受输出
     */
    public static BigInteger calculateMinOutput(BigInteger amountInWei, int slippagePercent) {
        BigInteger numerator = BigInteger.valueOf(100 - slippagePercent);
        BigInteger denominator = BigInteger.valueOf(100);
        return amountInWei.multiply(numerator).divide(denominator);
    }

    /**
     * BigDecimal 版本（用于可读金额 + 精度）
     *
     * @param amount          原始金额（例如 1.23）
     * @param decimals        小数精度（例如 ERC20 token 通常是 18）
     * @param slippagePercent 滑点百分比（例如 1 表示 1%）
     * @return 最小可接受输出（单位：最小单位）
     */
    public static BigInteger calculateMinOutput(BigDecimal amount, int decimals, int slippagePercent) {
        BigDecimal factor = BigDecimal.TEN.pow(decimals);
        BigDecimal amountInWei = amount.multiply(factor);
        BigDecimal min = amountInWei.multiply(BigDecimal.valueOf(100 - slippagePercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        return min.toBigIntegerExact();
    }
}
