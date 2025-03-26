package cn.com.vortexa.browser_control.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SeleniumProxyAuth {

    private static final String BASE_DIR = System.getProperty("user.dir") + File.separator + "botData" + File.separator + "extensions";

    public static String createProxyAuthExtension(String host, int port, String user, String pass) throws IOException {
        String pluginPath = BASE_DIR + File.separator + "proxy_auth_plugin_%s_%s.zip".formatted(host, port);

        if (new File(pluginPath).exists()) {
            return pluginPath;
        }

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
                                  bypassList: ["foobar.com"]
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

        Path dirPath = Paths.get(BASE_DIR + File.separator + "proxy_auth_extension_%s_%s".formatted(host, port));

        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        File dir = dirPath.toFile();
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
