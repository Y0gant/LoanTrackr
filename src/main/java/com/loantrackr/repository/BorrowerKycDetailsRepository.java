package com.loantrackr.repository;

import com.loantrackr.model.BorrowerKycDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowerKycDetailsRepository extends JpaRepository<BorrowerKycDetails, Long> {
}