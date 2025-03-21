package cn.com.vortexa.control.constant;

public enum LoadBalancePolicy {
    ROUND_ROBIN,
    RANDOM,
    LEAST_CONNECTIONS,
    IP_HASH,
    WEIGHTED_ROUND_ROBIN,
    WEIGHTED_RANDOM,
    LEAST_RESPONSE_TIME;
}
