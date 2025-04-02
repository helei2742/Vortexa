//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//
//public class SHA256BruteForce {
//
//    // 计算 SHA-256 哈希值
//    public static String calculateSHA256(String input) {
//        try {
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] hashBytes = digest.digest(input.getBytes());
//            StringBuilder hexString = new StringBuilder();
//            for (byte b : hashBytes) {
//                hexString.append(String.format("%02x", b));
//            }
//            return hexString.toString();
//        } catch (NoSuchAlgorithmException e) {
//            throw new RuntimeException("SHA-256 algorithm not found", e);
//        }
//    }
//
//    // 暴力破解过程，尝试寻找符合条件的数字
//    public static int findMatchingNumber(String challenge, String salt, int maxNumber, String targetSignature) {
//        for (int number = 0; number < maxNumber; number++) {
//            // 拼接 challenge, salt 和当前数字
//            String input = challenge + salt + number;
//            // 计算 SHA-256 哈希值
//            String hashResult = calculateSHA256(input);
//            // 检查哈希值是否与目标 signature 匹配
//            if (hashResult.equals(targetSignature)) {
//                return number; // 返回找到的数字
//            }
//        }
//        return -1; // 如果没有找到匹配的数字
//    }
//
//    public static void main(String[] args) {
//        // 数据从 JSON 中提取
//        String challenge = "491f11f1edb5848743ffe96286b6cf591ce3aed69a210613cefe36c364e5d45f";
//        String salt = "a590401831d965550a1a3292";
//        int maxNumber = 500000;  // 最大数字限制
//        String targetSignature = "e5f4dcc0ef3231c343c52e100ecca68f8f2a10f9541501878348d8841b24f597";
//
//        // 记录开始时间
//        long startTime = System.currentTimeMillis();
//
//        // 进行暴力破解
//        int result = findMatchingNumber(challenge, salt, maxNumber, targetSignature);
//
//        // 记录结束时间
//        long endTime = System.currentTimeMillis();
//
//        // 输出结果
//        if (result != -1) {
//            System.out.println("Found matching number: " + result);
//        } else {
//            System.out.println("No matching number found.");
//        }
//
//        System.out.println("Time taken: " + (endTime - startTime) + " ms");
//    }
//}
