package com.loantrackr.repository;

import com.loantrackr.enums.LoanStatus;
import com.loantrackr.model.LenderProfile;
import com.loantrackr.model.Loan;
import com.loantrackr.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByBorrowerAndStatus(User borrower, LoanStatus status);

    List<Loan> findByLenderAndStatus(LenderProfile lender, LoanStatus status);

    List<Loan> findByStatus(LoanStatus status);

    @Query("SELECT l FROM Loan l WHERE l.nextDueDate = :dueDate AND l.status = :status")
    List<Loan> findByNextDueDateAndStatus(@Param("dueDate") LocalDate dueDate,
                                          @Param("status") LoanStatus status);

    List<Loan> findByLenderIdAndStatus(Long lenderId, LoanStatus status);


}