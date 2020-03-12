package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.enums.Status;
import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.utils.RedisDistributedLock;
import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.transaction.InvalidTransactionException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

class WalletServiceTest {

    private WalletService walletService;

    @BeforeEach
    private void setUp() {
        walletService = new WalletService();
    }

    @Test
    void should_change_the_status_to_expired_when_the_create_time_is_passed_20days() throws NoSuchFieldException, IllegalAccessException, InvalidTransactionException {
        WalletTransaction walletTransaction = new WalletTransaction("", 1L, 2L, 3);
        Field createdTimestamp = WalletTransaction.class.getDeclaredField("createdTimestamp");
        createdTimestamp.setAccessible(true);
        createdTimestamp.set(walletTransaction, LocalDateTime.now().minusDays(20).minusMinutes(1));

        walletService.execute(walletTransaction);

        assertEquals(Status.EXPIRED, walletTransaction.getStatus());
    }

    @Test
    void should_return_false_when_lock_failed() throws InvalidTransactionException {
        new MockUp<RedisDistributedLock>() {
            @Mock
            public boolean lock(String transactionId) {
                return false;
            }
        };

        assertFalse(walletService.execute(new WalletTransaction("", 1L, 2L, 3)));
    }

    @Test
    void should_set_status_to_executed_when_execute_success() throws InvalidTransactionException {
        new MockUp<WalletService>() {
            @Mock
            private boolean moveMoney(WalletTransaction walletTransaction) {
                return true;
            }
        };
        walletService = new WalletService();
        WalletTransaction walletTransaction = new WalletTransaction("", 1L, 2L, 3);
        walletService.execute(walletTransaction);
        assertEquals(Status.EXECUTED, walletTransaction.getStatus());
    }

    @Test
    void should_set_status_to_failed_when_execute_failed() throws InvalidTransactionException {
        new MockUp<WalletService>() {
            @Mock
            private boolean moveMoney(WalletTransaction walletTransaction) {
                return false;
            }
        };
        walletService = new WalletService();
        WalletTransaction walletTransaction = new WalletTransaction("", 1L, 2L, 3);
        walletService.execute(walletTransaction);
        assertEquals(Status.FAILED, walletTransaction.getStatus());
    }

    @Test
    void should_throw_exception_when_transaction_buyer_id_is_null() {
        try {
            walletService.execute(new WalletTransaction("", null, 2L, 3));
        } catch (InvalidTransactionException e) {
            return;
        }
        fail();
    }

    @Test
    void should_throw_exception_when_transaction_seller_id_is_null() {
        try {
            walletService.execute(new WalletTransaction("", 1L, null, 3));
        } catch (InvalidTransactionException e) {
            return;
        }
        fail();
    }

    @Test
    void should_throw_exception_when_transaction_amount_is_invalid() {
        try {
            walletService.execute(new WalletTransaction("", 1L, 2L, -1));
        } catch (InvalidTransactionException e) {
            return;
        }
        fail();
    }
}