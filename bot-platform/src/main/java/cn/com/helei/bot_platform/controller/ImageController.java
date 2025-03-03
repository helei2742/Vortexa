package cn.com.helei.bot_platform.controller;

import cn.com.helei.common.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Slf4j
@RestController
@RequestMapping("/image")
public class ImageController {

    @GetMapping("/app")
    public ResponseEntity<Resource> getImage(@RequestParam(name = "name") String name) throws IOException {
        String filePath = FileUtil.getBotAppConfigPath() + File.separator + name;
        log.info("request app dir image [{}]", filePath);

        Path path = Paths.get(filePath);
        Resource resource = new UrlResource(path.toUri());

        if (resource.exists()) {
            // 根据文件扩展名设置正确的 MIME 类型
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream"; // 默认类型
            }

            // 设置响应头
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType)) // 设置 MIME 类型
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + name + "\"")
                    .body(resource);
        } else {
            return ResponseEntity.notFound().build(); // 文件未找到时返回 404
        }
    }
}
