package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.enums.Status;
import cn.xpbootcamp.legacy_code.utils.IdGenerator;

import java.time.LocalDateTime;

public class WalletTransaction {
    private String id;
    private Long buyerId;
    private Long sellerId;
    private LocalDateTime createdTimestamp;
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
        this.createdTimestamp = LocalDateTime.now();
        this.amount = amount;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public LocalDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }

    public double getAmount() {
        return amount;
    }

    public Status getStatus() {
        return status;
    }
}