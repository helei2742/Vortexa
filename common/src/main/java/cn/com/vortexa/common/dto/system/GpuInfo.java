package cn.com.vortexa.common.dto.system;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GpuInfo {

    private String renderer;

    private String vendor;
}
