package cn.com.vortexa.web3;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;

import java.math.BigInteger;

/**
 * @author helei
 * @since 2025/3/28 11:16
 */
public class EthWalletUtil {

    public static Sign.SignatureData signatureMessage(String privateKey, String message) {
        ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
        byte[] messageBytes = Hash.sha3(message.getBytes(UTF_8));
        return Sign.signMessage(messageBytes, ecKeyPair, false);
    }
}
