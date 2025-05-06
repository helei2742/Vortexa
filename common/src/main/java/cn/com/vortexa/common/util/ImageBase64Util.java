package cn.com.vortexa.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

public class ImageBase64Util {
    private static final long MAX_IMAGE_SIZE = 102_400; // 100KB

    /**
     * 将图片文件读取为 Base64 字符串
     *
     * @param imagePath 图片文件路径
     * @return Base64 编码字符串（不包含前缀）
     * @throws IOException 文件读取失败时抛出
     */
    public static String imageToBase64(String imagePath) throws IOException {
        File file = new File(imagePath);

        if (!file.exists()) {
            throw new IOException("文件不存在: " + imagePath);
        }

        if (file.length() > MAX_IMAGE_SIZE) {
            throw new IOException("图片超过大小限制（100KB）: " + file.length() + " 字节");
        }

        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int read = inputStream.read(bytes);
            if (read != bytes.length) {
                throw new IOException("读取图片数据不完整");
            }
            return Base64.getEncoder().encodeToString(bytes);
        }
    }

    /**
     * png转base64
     *
     * @param imagePath imagePath
     * @return String
     * @throws IOException IOException
     */
    public static String pngToBase64DataUrl(String imagePath) throws IOException {
        return imageToBase64DataUrl(imagePath, "image/png");
    }

    /**
     * 返回带前缀的 Base64，用于 <img src="..."> 直接使用
     */
    public static String imageToBase64DataUrl(String imagePath, String mimeType) throws IOException {
        String base64 = imageToBase64(imagePath);
        return "data:" + mimeType + ";base64," + base64;
    }
}
