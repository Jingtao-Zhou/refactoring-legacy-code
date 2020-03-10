package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.enums.Status;
import org.junit.jupiter.api.Test;

import javax.transaction.InvalidTransactionException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletTransactionTest {
    @Test
    void should_change_the_status_to_expired_when_the_create_time_is_passed_20days() throws NoSuchFieldException, IllegalAccessException, InvalidTransactionException {
        WalletTransaction walletTransaction = new WalletTransaction("", 1L, 2L, 3);
        Field createdTimestamp = WalletTransaction.class.getDeclaredField("createdTimestamp");
        Field status = WalletTransaction.class.getDeclaredField("status");
        createdTimestamp.setAccessible(true);
        status.setAccessible(true);
        createdTimestamp.set(walletTransaction, System.currentTimeMillis() - 20 * 24 * 60 * 60 * 1000 - 1);
        walletTransaction.execute();
        assertEquals(Status.EXPIRED, status.get(walletTransaction));
    }
}