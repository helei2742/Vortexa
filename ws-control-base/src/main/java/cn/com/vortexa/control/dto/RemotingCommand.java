package cn.com.vortexa.control.dto;

import cn.com.vortexa.control.constant.LanguageCode;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.constant.RemotingCommandFlagConstants;
import cn.com.vortexa.control.protocol.Serializer;
import cn.com.vortexa.control.util.DistributeIdMaker;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemotingCommand {

    public static final RemotingCommand TIME_OUT_COMMAND;
    public static final RemotingCommand PARAMS_ERROR;

    public static final String TRANSACTION_ID_KEY = "transaction_id";
    public static final String GROUP_KEY = "group";
    public static final String SERVICE_ID_KEY = "service_id";
    public static final String CLIENT_ID_KEY = "client_id";

    static {
        TIME_OUT_COMMAND = new RemotingCommand();
        TIME_OUT_COMMAND.setFlag(RemotingCommandFlagConstants.TIME_OUT_EXCEPTION);
        TIME_OUT_COMMAND.setCode(RemotingCommandCodeConstants.FAIL);

        PARAMS_ERROR = new RemotingCommand();
        PARAMS_ERROR.setFlag(RemotingCommandFlagConstants.PARAMS_ERROR);
        PARAMS_ERROR.setCode(RemotingCommandCodeConstants.FAIL);
    }

    private Integer flag;
    private Integer code;
    private LanguageCode language = LanguageCode.JAVA;
    private Integer version = 0;
    private String remark;
    private HashMap<String, String> extFields;

    private byte[] body;

    private Object payLoad;

    @Override
    public RemotingCommand clone() {
        RemotingCommand remotingCommand = new RemotingCommand();
        remotingCommand.setFlag(this.flag);
        remotingCommand.setCode(this.code);
        remotingCommand.setLanguage(this.language);
        remotingCommand.setVersion(this.version);
        remotingCommand.setRemark(this.remark);
        remotingCommand.setExtFields(this.extFields);
        remotingCommand.setBody(this.body);
        remotingCommand.setPayLoad(this.payLoad);
        return remotingCommand;
    }

    public String getTransactionId() {
        return getExtFieldsValue(TRANSACTION_ID_KEY);
    }

    public void setTransactionId(String tsId) {
        addExtField(TRANSACTION_ID_KEY, tsId);
    }

    public String getGroup() {
        String value = getExtFieldsValue(GROUP_KEY);
        return (value == null || value.isEmpty()) ? "default" : value;
    }

    public void setGroup(String group) {
        addExtField(GROUP_KEY, group);
    }

    public String getServiceId() {
        return getExtFieldsValue(SERVICE_ID_KEY);
    }

    public void setServiceId(String serviceId) {
        addExtField(SERVICE_ID_KEY, serviceId);
    }

    public String getClientId() {
        return getExtFieldsValue(CLIENT_ID_KEY);
    }

    public void setClientId(String clientId) {
        addExtField(CLIENT_ID_KEY, clientId);
    }

    public String getExtFieldsValue(String extFieldsKey) {
        if (extFields == null) return null;
        return extFields.get(extFieldsKey);
    }

    public Integer getExtFieldsInt(String extFieldsKey) {
        String value = getExtFieldsValue(extFieldsKey);
        if (value == null || value.isEmpty()) return null;
        return Integer.parseInt(value);
    }

    public Long getExtFieldsLong(String extFieldsKey) {
        String value = getExtFieldsValue(extFieldsKey);
        if (value == null || value.isEmpty()) return null;
        return Long.parseLong(value);
    }

    public void addExtField(String key, String value) {
        if (this.extFields == null) {
            this.extFields = new HashMap<>();
        }
        this.extFields.put(key, value);
    }

    public void setBodyFromObject(Object body) {
        this.body = Serializer.Algorithm.Protostuff.serialize(body);
    }

    public void release() {
    }

    protected void clear() {
        this.flag = null;
        this.code = null;
        this.language = null;
        this.version = null;
        this.remark = null;
        if (extFields == null) extFields = new HashMap<>();
        else this.extFields.clear();
        this.body = null;
    }

    public static RemotingCommand generatePingCommand(String key) {
        String txId = DistributeIdMaker.DEFAULT.nextId(key);
        RemotingCommand ping = new RemotingCommand();
        ping.setFlag(RemotingCommandFlagConstants.PING);
        ping.setTransactionId(txId);
        ping.setCode(RemotingCommandCodeConstants.SUCCESS);
        return ping;
    }
    public static RemotingCommand generatePongCommand(String key) {
        String txId = DistributeIdMaker.DEFAULT.nextId(key);
        RemotingCommand pong = new RemotingCommand();
        pong.setFlag(RemotingCommandFlagConstants.PONG);
        pong.setTransactionId(txId);
        pong.setCode(RemotingCommandCodeConstants.SUCCESS);
        return pong;
    }

    @Override
    public String toString() {
        return "RemotingCommand{" +
                "flag=" + flag +
                ", code=" + code +
                ", language=" + language +
                ", version=" + version +
                ", remark='" + remark + '\'' +
                ", extFields=" + extFields +
                ", body=" + ((body == null || body.length == 0) ? "empty" : "not empty") +
                '}';
    }
}
