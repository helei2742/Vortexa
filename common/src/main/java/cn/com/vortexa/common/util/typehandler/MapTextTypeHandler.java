package cn.com.vortexa.common.util.typehandler;

import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MapTextTypeHandler extends BaseTypeHandler<Map<String, Object>> {


    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType) throws SQLException {

        String jsonString = parameter != null ? JSONObject.toJSONString(parameter) : "";
        ps.setString(i, jsonString);
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        return JSONObject.parseObject(json, Map.class);
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return JSONObject.parseObject(json, Map.class);
    }

    @Override
    public Map<String, Object> getNullableResult(java.sql.CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return JSONObject.parseObject(json, Map.class);
    }
//
//    @Override
//    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType) throws SQLException {
//        String jsonString = null;
//        try {
//            jsonString = parameter != null ? encode(parameter) : "";
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        ps.setString(i, jsonString);
//    }
//
//    @Override
//    public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
//        String string = rs.getString(columnName);
//        if (string == null || string.isEmpty()) {
//            return new HashMap<>();
//        }
//        try {
//            return decode(string);
//        } catch (IOException | ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Override
//    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
//        String string = rs.getString(columnIndex);
//        if (string == null || string.isEmpty()) {
//            return new HashMap<>();
//        }
//        try {
//            return decode(string);
//        } catch (IOException | ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Override
//    public Map<String, Object> getNullableResult(java.sql.CallableStatement cs, int columnIndex) throws SQLException {
//        String string = cs.getString(columnIndex);
//        if (string == null || string.isEmpty()) {
//            return new HashMap<>();
//        }
//        try {
//            return decode(string);
//        } catch (IOException | ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public String encode(Map<String, Object> parameter) throws IOException {
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
//        objectOutputStream.writeObject(parameter);
//        objectOutputStream.flush();
//        byte[] serializedBytes = byteArrayOutputStream.toByteArray();
//
//        // 将字节数组转为 Base64 字符串
//        return Base64.getEncoder().encodeToString(serializedBytes);
//    }
//
//    private Map<String, Object> decode(String serializedString) throws IOException, ClassNotFoundException {
//        if (serializedString == null || serializedString.isEmpty()) {
//            return new HashMap<>();
//        }
//        // 反序列化过程，将 Base64 字符串转换回 Map
//        byte[] decodedBytes = Base64.getDecoder().decode(serializedString);
//        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decodedBytes);
//        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
//        return  (Map<String, Object>) objectInputStream.readObject();
//    }
}
