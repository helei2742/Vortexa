package cn.com.vortexa.web3;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.web3j.crypto.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.utils.Numeric;
import org.web3j.crypto.Keys;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * @author helei
 * @since 2025/3/28 11:16
 */
public class EthWalletUtil {

    private static final SecureRandom secureRandom = new SecureRandom();


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

//0x729224010a08baa5e51f7faa1a7d5a221c30c8c24789c9b7291db3f23110b866594e9436826af7035b071c582bb6c1226c27476362f08b460ee2ff7032a9c0d41c
public static void main(String[] args) {
    try {
        // 要签名的消息
        String message = "klokapp.ai wants you to sign in with your Ethereum account:\n0x2dB603E747E2db72747E5b972006f19B2D0d73a1\n\n\nURI: https://klokapp.ai/\nVersion: 1\nChain ID: 1\nNonce: b1bafc14190f3b2083f3cc02dec3c33fb56dd5021b9cc9eef59d4d896d2267127cd3a4ba8983cd0f0158b841fab2434b\nIssued At: 2025-04-02T14:27:46.541Z";

;
 String target = "0xe0d8facfd1052646c98feb0aca7a703fc4d0b8699d2405bf074a40222ca69caa7c45f6f17274c7afd7a59b962cd66a3b89eeae207d33947350719b86636c45f21c";
        // 使用私钥创建 ECKeyPair
        String privateKey = "6f31cabe993df1c6e377a3c8e9fd42f92c587a3925a4a8fa1f7be4aee6eff6e6";  // 替换为你的私钥
//        byte[] contentHashBytes = message.getBytes();
//        // 根据私钥获取凭证对象
//        Credentials credentials = Credentials.create(privateKey);
//        //
//        Sign.SignatureData signMessage = Sign.signPrefixedMessage(contentHashBytes, credentials.getEcKeyPair());
//
//        byte[] r = signMessage.getR();
//        byte[] s = signMessage.getS();
//        byte[] v = signMessage.getV();
//
//        byte[] signByte = Arrays.copyOf(r, v.length + r.length + s.length);
//        System.arraycopy(s, 0, signByte, r.length, s.length);
//        System.arraycopy(v, 0, signByte, r.length + s.length, v.length);
//        Numeric.toHexString(signByte)

        System.out.println("签名后的消息: " + signatureMessage2String(privateKey, message));
    } catch (Exception e) {
        e.printStackTrace();
    }
}
}
