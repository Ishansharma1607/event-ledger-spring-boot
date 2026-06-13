package com.mphasis.eventledger.account.repository;

import com.mphasis.eventledger.account.domain.AccountTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountTransactionRepository extends JpaRepository<AccountTransactionEntity, String> {

    List<AccountTransactionEntity> findByAccountIdOrderByEventTimestampAscEventIdAsc(String accountId);
}
