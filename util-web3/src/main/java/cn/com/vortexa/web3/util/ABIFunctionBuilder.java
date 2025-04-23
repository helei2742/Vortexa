package cn.com.vortexa.web3.util;

import cn.com.vortexa.web3.constants.Web3jFunctionType;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author h30069248
 * @since 2025/4/23 10:51
 */
public class ABIFunctionBuilder {
    private String functionName;
    private final List<Type> parameterTypes = new ArrayList<>();
    private final List<TypeReference<?>> returnTypes = new ArrayList<>();

    public static ABIFunctionBuilder builder() {
        return new ABIFunctionBuilder();
    }

    public ABIFunctionBuilder functionName(String functionName) {
        this.functionName = functionName;
        return this;
    }

    public ABIFunctionBuilder addParameterType(Web3jFunctionType type, Object value) {
        parameterTypes.add(switch (type) {
            case Uint256 -> new Uint256(BigInteger.valueOf((Long) value));
            case Address -> new Address(String.valueOf(value));
        });
        return this;
    }

    public ABIFunctionBuilder addReturnType(Web3jFunctionType type) {
        returnTypes.add(switch (type) {
            case Uint256 -> new TypeReference<Uint256>() {};
            case Address -> new TypeReference<Address>() {};
        });
        return this;
    }

    public Function build() {
        return new Function(functionName, parameterTypes, returnTypes);
    }
}
