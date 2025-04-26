package cn.com.vortexa.web3.util;

import cn.com.vortexa.web3.constants.Web3jFunctionType;

import lombok.Getter;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Int128;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author com.helei
 * @since 2025/4/23 10:51
 */
@Getter
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
            case Int128 -> new Int128((BigInteger) value);
            case Uint256 -> new Uint256((BigInteger) value);
            case Bool -> new Bool((Boolean) value);
            case Address -> new Address(String.valueOf(value));
        });
        return this;
    }

    public ABIFunctionBuilder addReturnType(Web3jFunctionType type) {
        TypeReference<?> e = switch (type) {
            case Int128 -> TypeReference.create(Int128.class);
            case Uint256 -> TypeReference.create(Uint256.class);
            case Bool -> TypeReference.create(Bool.class);
            case Address -> TypeReference.create(Address.class);
        };

        returnTypes.add(e);
        return this;
    }

    public Function build() {
        return new Function(functionName, parameterTypes, returnTypes);
    }
}
