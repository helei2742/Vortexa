package cn.com.vortexa.script_node.service;

import com.baomidou.mybatisplus.extension.service.IService;

import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.entity.RewordInfo;

import java.sql.SQLException;
import java.util.HashMap;

public interface IRewordInfoService extends IService<RewordInfo> {

    /**
     * 查找并传几个分表
     *
     * @param id id
     * @param botKey botKey
     * @return boolean
     */
    boolean checkAndCreateShardedTable(Integer id, String botKey) throws SQLException;

    /**
     * 查询账户收益
     *
     * @param pageNum pageNum
     * @param pageSize pageSize
     * @param params params
     * @return String
     */
    PageResult<RewordInfo> queryAccountReword(Integer pageNum, Integer pageSize, HashMap<String, Object> params);
}
