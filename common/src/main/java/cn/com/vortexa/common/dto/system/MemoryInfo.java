package cn.com.vortexa.common.dto.system;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemoryInfo {

    private Long availableCapacity;

    private Long capacity;
}
