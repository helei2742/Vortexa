package cn.com.vortexa.common.util.aws;

import cn.hutool.core.codec.Base64;
import lombok.Getter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;


public class SRPHelper {
    private static final BigInteger N = new BigInteger( // SRP-1024 大素数
            "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A" +
                    "431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5" +
                    "AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62" +
                    "F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2" +
                    "EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D0450" +
                    "7A33A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7ABF5AE8CDB0933D71E8C94E04A25619D" +
                    "CEE3D2261AD2EE6BF12FFA06D98A0864D87602733EC86A64521F2B18177B200CBBE117577A615D6C770988C0BAD946E208E2" +
                    "4FA074E5AB3143DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF", 16);

    private static final BigInteger g = BigInteger.valueOf(2);

    private static final SecureRandom random = new SecureRandom();

    private static final String hmacSha256 = "HmacSHA256";

    private static final String DERIVED_KEY_INFO = "Caldera Derived Key";

    private static final int DERIVED_KEY_SIZE = 16;

    private BigInteger k;

    @Getter
    private BigInteger privateA;

    @Getter
    private BigInteger publicA;

    @Getter
    private String dateString;

    public MessageDigest digest = MessageDigest.getInstance("SHA256");

    private String userId;

    private String password;

    private String userPoolName;

    public SRPHelper(String password) throws NoSuchAlgorithmException {
        this.password = password;


        do {
            privateA = new BigInteger(1024, random).mod(N);
            // A = (g ^ a) % N
            publicA = g.modPow(privateA, N);
        } while (publicA.mod(N).equals(BigInteger.ZERO));

        digest.reset();
        digest.update(N.toByteArray());

        k = new BigInteger(1, digest.digest(g.toByteArray()));

        dateString = getFormattedTimestamp();
    }


    public void setUserId(String userId, String userPoolName) {
        this.userId = userId;
        this.userPoolName = userPoolName;
        if (userPoolName.contains("_")) {
            this.userPoolName = userPoolName.split("_", 2)[1];
        }
    }

    // u = H(A, B)
    private BigInteger computeU(BigInteger srpB) {
        digest.reset();
        digest.update(publicA.toByteArray());
        return new BigInteger(1, digest.digest(srpB.toByteArray()));
    }

    // x = H(salt | H(poolName | userId | ":" | password))
    private BigInteger computeX(BigInteger salt) {
        digest.reset();
        digest.update(userPoolName.getBytes());
        digest.update(userId.getBytes());
        digest.update(":".getBytes());
        byte[] userIdPasswordHash = digest.digest(password.getBytes());

        digest.reset();
        digest.update(salt.toByteArray());
        return new BigInteger(1, digest.digest(userIdPasswordHash));
    }


    // s = ((B - k * (g ^ x) % N) ^ (a + u * x) % N) % N
    private BigInteger computeS(BigInteger uValue, BigInteger xValue, BigInteger srpB) {
        return (srpB.subtract(k.multiply(g.modPow(xValue, N))).modPow(privateA.add(uValue.multiply(xValue)), N)).mod(N);
    }

    // p = MAC("Caldera Derived Key" | 1, MAC(s, u))[0:16]
    private byte[] computePasswordAuthenticationKey(BigInteger ikm, BigInteger salt) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(hmacSha256);
        SecretKeySpec keySpec = new SecretKeySpec(salt.toByteArray(), hmacSha256);
        mac.init(keySpec);
        byte[] prk = mac.doFinal(ikm.toByteArray());

        mac.reset();
        keySpec = new SecretKeySpec(prk, hmacSha256);
        mac.init(keySpec);
        mac.update(DERIVED_KEY_INFO.getBytes());
        byte[] hkdf = mac.doFinal(String.valueOf((char) 1).getBytes());
        return Arrays.copyOf(hkdf, 16);
    }

    private byte[] generateM1Signature(byte[] key, String secretBlock) throws InvalidKeyException, NoSuchAlgorithmException {
        Mac mac = Mac.getInstance(hmacSha256);
        SecretKeySpec keySpec = new SecretKeySpec(key, hmacSha256);
        mac.init(keySpec);
        mac.update(userPoolName.getBytes());
        mac.update(userId.getBytes());
        mac.update(Base64.decode(secretBlock));

        return mac.doFinal(dateString.getBytes());
    }

    public String getSignature(String salt, String srpB, String secretBlock) throws NoSuchAlgorithmException, InvalidKeyException {
        BigInteger bigIntSRPB = new BigInteger(srpB, 16);
        BigInteger bigIntSalt = new BigInteger(salt, 16);

        // Check B's validity
        if (bigIntSRPB.mod(N).equals(BigInteger.ZERO))
            throw new RuntimeException("Bad server public value 'B'");

        BigInteger uValue = computeU(bigIntSRPB);
        if (uValue.mod(N).equals(BigInteger.ZERO))
            throw new RuntimeException("Hash of A and B cannot be zero");

        BigInteger xValue = computeX(bigIntSalt);
        BigInteger sValue = computeS(uValue, xValue, bigIntSRPB);
        byte[] key = computePasswordAuthenticationKey(sValue, uValue);
        byte[] m1Signature = generateM1Signature(key, secretBlock);
        return Base64.encode(m1Signature);
    }

    // 生成 Cognito 需要的时间戳格式
    private static String getFormattedTimestamp() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", java.util.Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new java.util.Date());
    }
}
