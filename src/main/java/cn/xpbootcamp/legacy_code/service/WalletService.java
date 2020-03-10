package cn.xpbootcamp.legacy_code.service;

import cn.xpbootcamp.legacy_code.WalletTransaction;
import cn.xpbootcamp.legacy_code.entity.User;
import cn.xpbootcamp.legacy_code.enums.Status;
import cn.xpbootcamp.legacy_code.repository.UserRepository;
import cn.xpbootcamp.legacy_code.repository.UserRepositoryImpl;
import cn.xpbootcamp.legacy_code.utils.RedisDistributedLock;

import javax.transaction.InvalidTransactionException;
import java.time.LocalDateTime;
import java.util.UUID;

public class WalletService {
    private UserRepository userRepository = new UserRepositoryImpl();

    private String moveMoney(String id, long buyerId, long sellerId, double amount) {
        User buyer = userRepository.find(buyerId);
        if (buyer.getBalance() >= amount) {
            User seller = userRepository.find(sellerId);
            seller.setBalance(seller.getBalance() + amount);
            buyer.setBalance(buyer.getBalance() - amount);
            return UUID.randomUUID().toString() + id;
        } else {
            return null;
        }
    }

    public boolean execute(WalletTransaction walletTransaction) throws InvalidTransactionException {
        if (walletTransaction.getBuyerId() == null
                || (walletTransaction.getSellerId() == null || walletTransaction.getAmount() < 0.0)) {
            throw new InvalidTransactionException("This is an invalid transaction");
        }
        if (walletTransaction.getStatus() == Status.EXECUTED) return true;
        boolean isLocked = false;
        try {
            isLocked = RedisDistributedLock.getSingletonInstance().lock(walletTransaction.getId());

            if (!isLocked) {
                return false;
            }
            if (createTimeHasPassed20days(walletTransaction)) {
                walletTransaction.setStatus(Status.EXPIRED);
                return false;
            }
            String walletTransactionId = this.moveMoney(
                    walletTransaction.getId(),
                    walletTransaction.getBuyerId(),
                    walletTransaction.getSellerId(),
                    walletTransaction.getAmount()
            );
            if (walletTransactionId != null) {
                walletTransaction.setStatus(Status.EXECUTED);
                return true;
            } else {
                walletTransaction.setStatus(Status.FAILED);
                return false;
            }
        } finally {
            if (isLocked) {
                RedisDistributedLock.getSingletonInstance().unlock(walletTransaction.getId());
            }
        }
    }

    private boolean createTimeHasPassed20days(WalletTransaction walletTransaction) {
        return LocalDateTime.now().isAfter(walletTransaction.getCreatedTimestamp().plusDays(20));
    }
}
