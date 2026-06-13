package com.mphasis.eventledger.account.service;

import com.mphasis.eventledger.account.domain.AccountEntity;
import com.mphasis.eventledger.account.domain.AccountTransactionEntity;
import com.mphasis.eventledger.account.observability.AccountMetrics;
import com.mphasis.eventledger.account.repository.AccountRepository;
import com.mphasis.eventledger.account.repository.AccountTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
public class AccountTransactionService {

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final AccountMetrics metrics;

    public AccountTransactionService(
            AccountRepository accountRepository,
            AccountTransactionRepository transactionRepository,
            AccountMetrics metrics
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.metrics = metrics;
    }

    @Transactional
    public ApplyTransactionResult apply(String accountId, ApplyTransactionCommand command) {
        if (!accountId.equals(command.accountId())) {
            throw new AccountIdMismatchException(accountId, command.accountId());
        }

        var duplicate = transactionRepository.findById(command.eventId());
        if (duplicate.isPresent()) {
            metrics.duplicateTransaction();
            var transaction = duplicate.get();
            var account = accountRepository.findById(transaction.getAccountId())
                    .orElseThrow(() -> new AccountNotFoundException(transaction.getAccountId()));
            return new ApplyTransactionResult(
                    false,
                    account.getAccountId(),
                    account.getBalance(),
                    account.getCurrency(),
                    TransactionView.from(transaction)
            );
        }

        var account = accountRepository.findById(accountId)
                .orElseGet(() -> new AccountEntity(accountId, command.currency()));
        if (!account.getCurrency().equals(command.currency())) {
            throw new CurrencyMismatchException(accountId, account.getCurrency(), command.currency());
        }

        account.apply(command.type(), command.amount());
        accountRepository.save(account);

        var transaction = new AccountTransactionEntity(
                command.eventId(),
                accountId,
                command.type(),
                command.amount(),
                command.currency(),
                command.eventTimestamp(),
                command.traceId()
        );
        transactionRepository.save(transaction);
        metrics.transactionApplied();

        return new ApplyTransactionResult(
                true,
                account.getAccountId(),
                account.getBalance(),
                account.getCurrency(),
                TransactionView.from(transaction)
        );
    }

    @Transactional(readOnly = true)
    public AccountDetails getAccount(String accountId) {
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        var transactions = transactionRepository.findByAccountIdOrderByEventTimestampAscEventIdAsc(accountId)
                .stream()
                .map(TransactionView::from)
                .sorted(Comparator.comparing(TransactionView::eventTimestamp).thenComparing(TransactionView::eventId))
                .toList();

        return new AccountDetails(
                account.getAccountId(),
                account.getBalance(),
                account.getCurrency(),
                account.getUpdatedAt(),
                transactions
        );
    }
}
