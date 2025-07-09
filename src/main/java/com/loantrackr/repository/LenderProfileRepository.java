package com.loantrackr.repository;

import com.loantrackr.model.LenderProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LenderProfileRepository extends JpaRepository<LenderProfile, Long> {
}