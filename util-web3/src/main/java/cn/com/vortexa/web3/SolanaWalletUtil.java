package cn.com.vortexa.web3;

import com.portto.solana.web3.util.TweetNaclFast;
import com.portto.solana.web3.wallet.DerivableType;
import com.portto.solana.web3.wallet.SolanaBip44;

import cn.com.vortexa.web3.dto.WalletInfo;
import cn.com.vortexa.web3.exception.SignatureException;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;

import org.bitcoinj.core.Base58;
import org.web3j.crypto.Bip39Wallet;
import org.web3j.crypto.Bip44WalletUtils;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Signature;

/**
 * @author helei
 * @since 2025/4/22 9:17
 */
public class SolanaWalletUtil {
    public static void main(String[] args) {
        String mnemonic = "replace fuel parent quarter lake pepper sweet gorilla bitter invest bike rude";
        System.out.println(generateWalletInfoFromMnemonic(mnemonic));
    }

    public static WalletInfo generateWalletInfoFromMnemonic(String mnemonic) {
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, "");
        SolanaBip44 solanaBip44 = new SolanaBip44();
        //M/44H/501H/0H/0H
        byte[] privateKeyFromSeed = solanaBip44.getPrivateKeyFromSeed(seed, DerivableType.BIP44CHANGE);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(privateKeyFromSeed);
        return WalletInfo.builder().mnemonic(mnemonic)
                .privateKey(Base58.encode(keyPair.getSecretKey()))
                .address(Base58.encode(keyPair.getPublicKey())).build();
    }

    public static Bip39Wallet generateBip44Wallet(String pwd, String dirPath) throws CipherException, IOException {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("make wallet dir error");
            }
        }
        return Bip44WalletUtils.generateBip44Wallet(pwd, dir);
    }

    public static String generateMnemonicFile(String pwd, String mnemonic, String dirPath) throws CipherException, IOException {
        // make dir
        File dir = new File(dirPath);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("make wallet dir error");
            }
        }
        // string to pk
        byte[] entBytes = MnemonicUtils.generateEntropy(mnemonic);
        BigInteger entBigInteger = new BigInteger(entBytes);
        ECKeyPair entEcKeyPair = ECKeyPair.create(entBigInteger);
        return WalletUtils.generateWalletFile(pwd, entEcKeyPair, dir, false);
    }


    public static String signatureMessage2String(String base58PrivateKey, String message) throws SignatureException {
        try {
            // 解码私钥
            byte[] privateKeyBytes = Base58.decode(base58PrivateKey);
            if (privateKeyBytes.length != 64) {
                throw new IllegalArgumentException("私钥长度必须是64字节，当前是 " + privateKeyBytes.length);
            }

            // 取前32字节作为种子
            byte[] seed = new byte[32];
            System.arraycopy(privateKeyBytes, 0, seed, 0, 32);

            // 构造 ed25519 私钥
            EdDSAPrivateKeySpec privateKeySpec = new EdDSAPrivateKeySpec(seed, EdDSANamedCurveTable.getByName("Ed25519"));
            EdDSAPrivateKey privateKey = new EdDSAPrivateKey(privateKeySpec);

            // 签名
            Signature signer = new EdDSAEngine();
            signer.initSign(privateKey);
            signer.update(message.getBytes(StandardCharsets.UTF_8));
            byte[] signature = signer.sign();

            // 返回 base58 编码签名
            return Base58.encode(signature);
        } catch (Exception e) {
            throw new SignatureException("签名失败: " + e.getMessage(), e);
        }
    }
}
