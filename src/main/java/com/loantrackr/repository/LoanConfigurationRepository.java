package com.loantrackr.repository;

import com.loantrackr.model.LoanConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LoanConfigurationRepository extends JpaRepository<LoanConfiguration, Long> {
    @Query("select l from LoanConfiguration l where l.active = :active")
    LoanConfiguration findByActive(boolean active);
}