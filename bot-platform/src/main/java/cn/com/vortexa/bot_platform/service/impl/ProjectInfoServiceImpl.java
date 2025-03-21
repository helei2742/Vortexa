package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.db_layer.service.AbstractBaseService;
import cn.com.vortexa.common.entity.ProjectInfo;
import cn.com.vortexa.db_layer.mapper.ProjectInfoMapper;
import cn.com.vortexa.db_layer.service.IProjectInfoService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-07
 */
@Service
public class ProjectInfoServiceImpl extends AbstractBaseService<ProjectInfoMapper, ProjectInfo> implements
        IProjectInfoService {

    public ProjectInfoServiceImpl() {
        super(projectInfo -> {
            projectInfo.setInsertDatetime(LocalDateTime.now());
            projectInfo.setUpdateDatetime(LocalDateTime.now());
            projectInfo.setIsValid(1);
        });
    }
}
