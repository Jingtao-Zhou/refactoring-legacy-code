package cn.xpbootcamp.legacy_code.utils;

public class RedisDistributedLock {
    private static final RedisDistributedLock INSTANCE = new RedisDistributedLock();

    public static RedisDistributedLock getSingletonInstance() {
        return INSTANCE;
    }

    public boolean lock(String transactionId) {
        return true;
    }

    public void unlock(String transactionId) {
    }
}
