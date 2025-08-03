package com.loantrackr.repository;

import com.loantrackr.enums.LoanStatus;
import com.loantrackr.model.LenderProfile;
import com.loantrackr.model.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    boolean existsByUserIdAndStatusIn(Long userId, List<LoanStatus> pending);

    List<LoanApplication> findByUserIdAndStatusIn(Long userId, List<LoanStatus> pending);

    Collection<LoanApplication> findByUserId(Long id);

    List<LoanApplication> findLoanApplicationByLender(LenderProfile lender);

    List<LoanApplication> findByLenderId(Long lenderId);

    List<LoanApplication> findByLenderIdAndStatus(Long lenderId, LoanStatus status);

    long countByLenderIdAndStatus(Long lenderId, LoanStatus status);

}