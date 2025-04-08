package cn.com.vortexa.common.util.protocol;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.util.List;

public interface Serializer {
    <T> T deserialize(byte[] bytes, Class<T> aClass);

    <T> byte[] serialize(T object);

    <T> List<T> deserializeList(byte[] bytes, Class<T> aClass);

    enum Algorithm implements Serializer {
        Protostuff {
            @Override
            public <T> T deserialize(byte[] bytes, Class<T> aClass) {
                return ProtostuffUtils.deserialize(bytes, aClass);
            }

            @Override
            public <T> byte[] serialize(T object) {
                return ProtostuffUtils.serialize(object);
            }

            @Override
            public <T> List<T> deserializeList(byte[] bytes, Class<T> aClass) {
                throw new IllegalStateException("Protostuff didn't support deserializeList method");
            }
        },
        JSON {
            @Override
            public <T> T deserialize(byte[] bytes, Class<T> aClass) {
                return JSONObject.parseObject(bytes, aClass);
            }

            @Override
            public <T> byte[] serialize(T object) {
                return JSONObject.toJSONBytes(object);
            }
            public <T> List<T> deserializeList(byte[] bytes, Class<T> aClass) {
                Object parse = JSONArray.parse(bytes);
                return JSONArray.parseArray(parse.toString(), aClass);
            }
        },
        JDK {
            @Override
            public <T> T deserialize(byte[] data, Class<T> aClass) {
                if (data == null || data.length == 0) {
                    throw new IllegalArgumentException("deserialize data is null or empty");
                }
                try (var bis = new ByteArrayInputStream(data);
                     ObjectInputStream ois = new ObjectInputStream(bis)) {
                    Object obj = ois.readObject();
                    return aClass.cast(obj);
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException("JDK deserialize error", e);
                }
            }

            @Override
            public <T> byte[] serialize(T object) {
                if (object == null) {
                    throw new IllegalArgumentException("serialize object is null");
                }
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                     ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(object);
                    return bos.toByteArray();
                } catch (IOException e) {
                    throw new RuntimeException("JDK serialize error", e);
                }
            }

            @Override
            public <T> List<T> deserializeList(byte[] bytes, Class<T> aClass) {
                throw new IllegalStateException("jsk didn't support deserializeList method");
            }
        }
    }
}
