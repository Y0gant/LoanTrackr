package com.loantrackr.repository;

import com.loantrackr.model.Loan;
import com.loantrackr.model.LoanPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanPaymentRepository extends JpaRepository<LoanPayment, Long> {
    List<LoanPayment> findByLoanOrderByCreatedAtDesc(Loan loan);
}
