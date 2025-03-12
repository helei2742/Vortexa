package cn.com.vortexa.common.dto;

import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;

@Data
public class AutoBotRuntimeInfo {

    private final ConcurrentHashMap<String, Object> keyValueInfoMap = new ConcurrentHashMap<>();

}
