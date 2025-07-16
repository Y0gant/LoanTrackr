package com.loantrackr.repository;

import com.loantrackr.model.BankDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankDetailsRepository extends JpaRepository<BankDetails, Long> {
}