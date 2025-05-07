package cn.com.vortexa.bot_platform.controller;


import cn.com.vortexa.bot_platform.service.IVersionService;
import cn.com.vortexa.bot_platform.vo.BotVersionVO;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.VersionUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author helei
 * @since 2025-05-06
 */
@RestController
@RequestMapping("/version")
public class VersionController {

    @Autowired
    private IVersionService versionService;

    @PostMapping("/botVersions")
    public Result botVersions(@RequestBody BotVersionVO botVersionVO) {
        return Result.ok(versionService.queryBotNewestVersions(botVersionVO.getBotNames()));
    }

    @GetMapping("/bot/download/{botName}/{version}")
    public ResponseEntity<Resource> downloadJarFile(@PathVariable("botName") String botName, @PathVariable("version") String version)
            throws MalformedURLException {
        String jarFileName = FileUtil.getLibraryPath(VersionUtil.getBotJarFileName(botName, version));
        Path jarPath = Paths.get(jarFileName);
        Resource resource = new UrlResource(jarPath.toUri());
        if (!resource.exists()) {
            throw new IllegalArgumentException("Jar File not found, " +  jarFileName);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
