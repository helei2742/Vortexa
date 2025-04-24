package cn.com.vortexa.web3.constants;

import cn.com.vortexa.common.util.YamlConfigLoadUtil;
import cn.com.vortexa.web3.dto.Web3ChainInfo;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author com.helei
 * @since 2025/4/23 10:27
 */
@Data
public class Web3ChainDict {
    public static final String RESOURCE_FILE_NAME = "chain-info.yaml";
    public static final List<String> PREFIX = List.of("vortexa", "web3");
    public static final Web3ChainDict INSTANCE;

    static {
        try (InputStream is = Web3ChainDict.class.getClassLoader().getResourceAsStream(RESOURCE_FILE_NAME)){
            INSTANCE = YamlConfigLoadUtil.load(RESOURCE_FILE_NAME, is, PREFIX, Web3ChainDict.class);
            if (INSTANCE.chainInfo != null) {
                INSTANCE.name2ChainInfoMap = INSTANCE.chainInfo.stream().collect(Collectors.toMap(Web3ChainInfo::getName, w->w));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Web3ChainDict loadCustomConfigDict(String path) {
        Web3ChainDict load = YamlConfigLoadUtil.load(new File(path), PREFIX, Web3ChainDict.class);
        if (load.chainInfo != null) {
            load.name2ChainInfoMap = load.chainInfo.stream().collect(Collectors.toMap(Web3ChainInfo::getName, w->w));
        }
        return load;
    }

    private List<Web3ChainInfo> chainInfo;

    private Map<String, Web3ChainInfo> name2ChainInfoMap;

    public Web3ChainInfo getChainInfo(String name) {
        return name2ChainInfoMap.get(name);
    }

    public static void main(String[] args) {
        System.out.println(INSTANCE);
    }

    public void marge(Web3ChainDict defaultChainDict) {
        if (name2ChainInfoMap == null) { name2ChainInfoMap = new HashMap<>();}

        List<Web3ChainInfo> list = defaultChainDict.getChainInfo();
        for (Web3ChainInfo web3ChainInfo : list) {
            String name = web3ChainInfo.getName();
            if (name2ChainInfoMap.containsKey(name)) {
                throw new IllegalArgumentException("chain [%s] already exist in default chain info dict".formatted(name));
            }
            name2ChainInfoMap.put(name, web3ChainInfo);
        }
    }
}
