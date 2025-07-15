package com.loantrackr.service;

import com.loantrackr.dto.request.LoanApplicationRequest;
import com.loantrackr.dto.response.EmiPreview;
import com.loantrackr.dto.response.LenderSummaryResponse;
import com.loantrackr.dto.response.LoanApplicationResponse;
import com.loantrackr.enums.LoanStatus;
import com.loantrackr.exception.OperationNotAllowedException;
import com.loantrackr.exception.UnauthorizedException;
import com.loantrackr.exception.UserNotFoundException;
import com.loantrackr.model.LenderProfile;
import com.loantrackr.model.LoanApplication;
import com.loantrackr.model.User;
import com.loantrackr.repository.LenderProfileRepository;
import com.loantrackr.repository.LoanApplicationRepository;
import com.loantrackr.util.LoanCalculatorUtil;
import com.loantrackr.util.SecurityUtils;
import com.loantrackr.util.TenureUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static com.loantrackr.util.LoanCalculatorUtil.*;

@Service
@AllArgsConstructor
public class LoanService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LenderProfileRepository lenderRepository;
    private final LenderProfileService lenderService;
    private final UserService userService;


    public List<LenderSummaryResponse> getAllLenders() {
        return lenderRepository.findAll().stream()
                .map(this::mapToSummary)
                .toList();
    }

    private LenderSummaryResponse mapToSummary(LenderProfile lender) {
        return LenderSummaryResponse.builder()
                .lenderId(lender.getId())
                .organizationName(lender.getOrganizationName())
                .interestRate(lender.getInterestRate())
                .processingFee(lender.getProcessingFee())
                .supportedTenures(TenureUtils.parseSupportedTenures(lender.getSupportedTenures()))
                .build();
    }


    public EmiPreview previewEmiFor(Long id, BigDecimal principal, int tenure) {
        LenderProfile lenderById = lenderService.getLenderById(id);
        if (!lenderById.isVerified() || !lenderById.getUser().isVerified() || !lenderById.getUser().isActive())
            throw new UnauthorizedException("Lender with id: " + id + " is not verified or isn't active");
        boolean tenureSupported = TenureUtils.isTenureSupported(lenderById.getSupportedTenures(), tenure);
        if (!tenureSupported)
            throw new OperationNotAllowedException("The lender doesn't support given tenure :" + tenure);
        EmiPreview preview = new EmiPreview();
        BigDecimal emi = calculateEMI(principal, lenderById.getInterestRate(), tenure);
        preview.setEmi(emi);
        preview.setTotalPayable(calculateTotalPayable(emi, tenure));
        preview.setTotalInterest(calculateTotalInterest(emi, principal, tenure));
        preview.setOrganization(lenderById.getOrganizationName());
        preview.setProcessingFee(lenderById.getProcessingFee());
        return preview;
    }

    public LoanApplicationResponse applyLoan(Long lenderId, LoanApplicationRequest request) {
        String userName = SecurityUtils.getCurrentUserName();
        User borrower = userService.getUserByUserName(userName)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // One-loan check
        boolean hasActiveLoan = loanApplicationRepository.existsByUserIdAndStatusIn(
                borrower.getId(),
                java.util.List.of(LoanStatus.PENDING, LoanStatus.APPROVED, LoanStatus.DISBURSED)
        );
        if (hasActiveLoan) throw new OperationNotAllowedException("Borrower already has an active loan.");


        LenderProfile lender = lenderService.getLenderById(lenderId);

        if (!lender.isVerified()) throw new UnauthorizedException("Lender is not verified");

        if (!TenureUtils.isTenureSupported(lender.getSupportedTenures(), request.getTenureInMonths())) {
            throw new OperationNotAllowedException("Selected tenure is not supported by this lender");
        }

        // Calculate EMI
        BigDecimal emi = LoanCalculatorUtil.calculateEMI(
                request.getLoanAmount(),
                lender.getInterestRate(),
                request.getTenureInMonths()
        );


        LoanApplication application = new LoanApplication();
        application.setUser(borrower);
        application.setLender(lender);
        application.setLoanRequested(request.getLoanAmount());
        application.setTenure(request.getTenureInMonths());
        application.setInterestRate(lender.getInterestRate());
        application.setProcessingFee(lender.getProcessingFee());
        application.setEmiAmount(emi);
        application.setStatus(LoanStatus.PENDING);
        loanApplicationRepository.save(application);


        return LoanApplicationResponse.builder()
                .applicationId(application.getId())
                .loanAmount(application.getLoanRequested())
                .tenure(application.getTenure())
                .emi(application.getEmiAmount())
                .interestRate(application.getInterestRate())
                .processingFee(application.getProcessingFee())
                .status(application.getStatus())
                .lenderName(lender.getOrganizationName())
                .appliedAt(application.getAppliedAt())
                .build();
    }
}
