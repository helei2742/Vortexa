package cn.com.helei.browser_control;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.*;

public class SeleniumProxyAuth {
    public static void main(String[] args) throws IOException {
        String PROXY_HOST = "46.203.161.123";
        int PROXY_PORT = 5620;
        String PROXY_USER = "hldjmuos";
        String PROXY_PASS = "545n41b7z20x";

        // 创建扩展文件
        String extensionFile = createProxyAuthExtension(PROXY_HOST, PROXY_PORT, PROXY_USER, PROXY_PASS);

        // 设置 ChromeOptions
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addExtensions(new File(extensionFile));

        WebDriver driver = new ChromeDriver(options);
        driver.get("http://www.example.com");
    }

    public static String createProxyAuthExtension(String host, int port, String user, String pass) throws IOException {
        String pluginPath = "proxy_auth_plugin.zip";

        String manifestJson = """
                {
                    "version": "1.0.0",
                    "manifest_version": 2,
                    "name": "Chrome Proxy",
                    "permissions": [
                        "proxy",
                        "tabs",
                        "unlimitedStorage",
                        "storage",
                        "<all_urls>",
                        "webRequest",
                        "webRequestBlocking"
                    ],
                    "background": {
                        "scripts": ["background.js"]
                    },
                    "minimum_chrome_version":"22.0.0"
                }
                """;

        String backgroundJs =
                """
                        var config = {
                                mode: "fixed_servers",
                                rules: {
                                  singleProxy: {
                                    scheme: "http",
                                    host: "%s",
                                    port: parseInt(%s)
                                  },
                                  bypassList: []
                                }
                              };
                        chrome.proxy.settings.set({value: config, scope: "regular"}, function() {});
                        function callbackFn(details) {
                            return {
                                authCredentials: {
                                    username: "%s",
                                    password: "%s"
                                }
                            };
                        }
                        chrome.webRequest.onAuthRequired.addListener(
                                    callbackFn,
                                    {urls: ["<all_urls>"]},
                                    ['blocking']
                        );
                        """.formatted(host, port, user, pass);

        File dir = new File("proxy_auth_extension");
        dir.mkdir();
        writeToFile(new File(dir, "manifest.json"), manifestJson);
        writeToFile(new File(dir, "background.js"), backgroundJs);

        // 创建 ZIP 扩展
        File zipFile = new File(pluginPath);
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(bos)) {

            addToZipFile(new File(dir, "manifest.json"), zos);
            addToZipFile(new File(dir, "background.js"), zos);
        }

        return pluginPath;
    }

    private static void writeToFile(File file, String content) throws IOException {
        try (FileWriter fw = new FileWriter(file);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(content);
        }
    }

    private static void addToZipFile(File file, java.util.zip.ZipOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = bis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
        }
    }
}
