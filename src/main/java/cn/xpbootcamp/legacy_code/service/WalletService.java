package cn.xpbootcamp.legacy_code.service;

import cn.xpbootcamp.legacy_code.WalletTransaction;
import cn.xpbootcamp.legacy_code.entity.User;
import cn.xpbootcamp.legacy_code.enums.Status;
import cn.xpbootcamp.legacy_code.repository.UserRepository;
import cn.xpbootcamp.legacy_code.repository.UserRepositoryImpl;
import cn.xpbootcamp.legacy_code.utils.RedisDistributedLock;

import javax.transaction.InvalidTransactionException;
import java.time.LocalDateTime;

public class WalletService {
    private UserRepository userRepository = new UserRepositoryImpl();

    public boolean execute(WalletTransaction walletTransaction) throws InvalidTransactionException {
        validateTransaction(walletTransaction);
        if (walletTransaction.getStatus() == Status.EXECUTED) return true;
        boolean isLocked;
        isLocked = RedisDistributedLock.getSingletonInstance().lock(walletTransaction.getId());
        if (!isLocked) {
            return false;
        }
        boolean result = verifyAndMoveMoney(walletTransaction);
        RedisDistributedLock.getSingletonInstance().unlock(walletTransaction.getId());
        return result;
    }

    private boolean verifyAndMoveMoney(WalletTransaction walletTransaction) {
        if (createTimeHasPassed20days(walletTransaction)) {
            walletTransaction.setStatus(Status.EXPIRED);
            return false;
        }
        boolean isMoveSuccess = this.moveMoney(walletTransaction);
        if (isMoveSuccess) {
            walletTransaction.setStatus(Status.EXECUTED);
            return true;
        } else {
            walletTransaction.setStatus(Status.FAILED);
            return false;
        }
    }

    private void validateTransaction(WalletTransaction walletTransaction) throws InvalidTransactionException {
        if (walletTransaction.getBuyerId() == null
            || (walletTransaction.getSellerId() == null
            || walletTransaction.getAmount() < 0.0)) {
            throw new InvalidTransactionException("This is an invalid transaction");
        }
    }

    private boolean createTimeHasPassed20days(WalletTransaction walletTransaction) {
        return LocalDateTime.now().isAfter(walletTransaction.getCreatedTimestamp().plusDays(20));
    }

    private boolean moveMoney(WalletTransaction walletTransaction) {
        User buyer = userRepository.find(walletTransaction.getBuyerId());
        if (buyer.getBalance() >= walletTransaction.getAmount()) {
            User seller = userRepository.find(walletTransaction.getSellerId());
            seller.setBalance(seller.getBalance() + walletTransaction.getAmount());
            buyer.setBalance(buyer.getBalance() - walletTransaction.getAmount());
            return true;
        } else {
            return false;
        }
    }
}
