package com.mphasis.eventledger.account.repository;

import com.mphasis.eventledger.account.domain.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, String> {
}
