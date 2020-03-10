package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.enums.Status;
import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.service.WalletServiceImpl;
import cn.xpbootcamp.legacy_code.utils.IdGenerator;
import cn.xpbootcamp.legacy_code.utils.RedisDistributedLock;

import javax.transaction.InvalidTransactionException;

public class WalletTransaction {
    private String id;
    private Long buyerId;
    private Long sellerId;
    private Long createdTimestamp;
    private double amount;
    private Status status;


    public WalletTransaction(String preAssignedId, Long buyerId, Long sellerId, double amount) {
        if (preAssignedId != null && !preAssignedId.isEmpty()) {
            this.id = preAssignedId;
        } else {
            this.id = IdGenerator.generateTransactionId();
        }
        if (!this.id.startsWith("t_")) {
            this.id = "t_" + preAssignedId;
        }
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.status = Status.TO_BE_EXECUTED;
        this.createdTimestamp = System.currentTimeMillis();
        this.amount = amount;
    }

    public boolean execute() throws InvalidTransactionException {
        if (buyerId == null || (sellerId == null || amount < 0.0)) {
            throw new InvalidTransactionException("This is an invalid transaction");
        }
        if (status == Status.EXECUTED) return true;
        boolean isLocked = false;
        try {
            isLocked = RedisDistributedLock.getSingletonInstance().lock(id);

            if (!isLocked) {
                return false;
            }
            if (createTimeHasPassed20days()) {
                this.status = Status.EXPIRED;
                return false;
            }
            WalletService walletService = new WalletServiceImpl();
            String walletTransactionId = walletService.moveMoney(id, buyerId, sellerId, amount);
            if (walletTransactionId != null) {
                this.status = Status.EXECUTED;
                return true;
            } else {
                this.status = Status.FAILED;
                return false;
            }
        } finally {
            if (isLocked) {
                RedisDistributedLock.getSingletonInstance().unlock(id);
            }
        }
    }

    private boolean createTimeHasPassed20days() {
        return System.currentTimeMillis() - createdTimestamp > 1728000000;
    }

}