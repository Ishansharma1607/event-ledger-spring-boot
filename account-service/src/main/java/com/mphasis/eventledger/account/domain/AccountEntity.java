package com.mphasis.eventledger.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    @Column(name = "account_id", nullable = false, updatable = false)
    private String accountId;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    protected AccountEntity() {
    }

    public AccountEntity(String accountId, String currency) {
        this.accountId = Objects.requireNonNull(accountId, "accountId");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.balance = BigDecimal.ZERO;
        this.updatedAt = Instant.now();
    }

    public void apply(TransactionType type, BigDecimal amount) {
        balance = switch (type) {
            case CREDIT -> balance.add(amount);
            case DEBIT -> balance.subtract(amount);
        };
        updatedAt = Instant.now();
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
