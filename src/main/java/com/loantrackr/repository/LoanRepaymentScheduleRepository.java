package com.loantrackr.repository;

import com.loantrackr.enums.LoanRepaymentStatus;
import com.loantrackr.model.Loan;
import com.loantrackr.model.LoanRepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LoanRepaymentScheduleRepository extends JpaRepository<LoanRepaymentSchedule, Long> {
    List<LoanRepaymentSchedule> findByLoanOrderByInstallmentNumber(Loan loan);

    Collection<LoanRepaymentSchedule> findByLoanAndStatus(Loan loan, LoanRepaymentStatus loanRepaymentStatus);
}