package com.loantrackr.repository;

import com.loantrackr.enums.RequestStatus;
import com.loantrackr.model.LenderOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LenderOnboardingRepository extends JpaRepository<LenderOnboarding, Long> {
    List<LenderOnboarding> findAllByStatus(RequestStatus status);

    List<LenderOnboarding> findAllByReviewedTrue();
}