package cn.com.vortexa.web3;

import cn.com.vortexa.web3.dto.WalletInfo;
import org.bitcoinj.crypto.*;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

/**
 * @author helei
 * @since 2025/3/28 11:16
 */
public class EthWalletUtil {

    private static final SecureRandom secureRandom = new SecureRandom();

    public static WalletInfo generateEthWallet() {
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
        String mnemonic = String.join(" ", mnemonicWords);
        // 2. 生成 seed
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, ""); // passphrase 可设置

        // 3. 创建 HD 钱包根节点 (BIP32 Root Key)
        DeterministicKey rootPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed);

        // 4. 分步派生 BIP44 路径 m/44'/60'/0'/0/0
        DeterministicKey purposeKey = HDKeyDerivation.deriveChildKey(rootPrivateKey, new ChildNumber(44, true));
        DeterministicKey coinTypeKey = HDKeyDerivation.deriveChildKey(purposeKey, new ChildNumber(60, true));
        DeterministicKey accountKey = HDKeyDerivation.deriveChildKey(coinTypeKey, new ChildNumber(0, true));
        DeterministicKey externalKey = HDKeyDerivation.deriveChildKey(accountKey, ChildNumber.ZERO); // change = 0
        DeterministicKey addressKey = HDKeyDerivation.deriveChildKey(externalKey, ChildNumber.ZERO); // address index = 0

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

    public static Sign.SignatureData signatureMessage2Data(String privateKey, String message) {
        byte[] contentHashBytes = message.getBytes();
        // 根据私钥获取凭证对象
        Credentials credentials = Credentials.create(privateKey);
        return Sign.signPrefixedMessage(contentHashBytes, credentials.getEcKeyPair());
    }

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

    public static String getETHAddress(String privateKeyHex) {
        Credentials credentials = Credentials.create(privateKeyHex);

        // 获取原始地址（小写）
        String rawAddress = credentials.getAddress();

        // 将地址转换为区分大小写的 Checksum 地址
        return Keys.toChecksumAddress(rawAddress);
    }


    public static BigInteger getNonce(String rpcUrl, String address) throws IOException {
        Web3j web3j = Web3j.build(new HttpService(rpcUrl));

        EthGetTransactionCount transactionCount = web3j.ethGetTransactionCount(
                        address, org.web3j.protocol.core.DefaultBlockParameterName.LATEST)
                .send();

        return transactionCount.getTransactionCount();
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
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));  // 格式化为两位十六进制
        }
        return hexString.toString();
    }

}
