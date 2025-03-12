package cn.com.vortexa.common.service;


import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface ImportService {

    Integer importFromExcel(String fileBotConfigPath) throws SQLException;

    Integer importFromRaw(List<Map<String, Object>> rawLines) throws SQLException;
}
