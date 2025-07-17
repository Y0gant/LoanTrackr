package com.loantrackr.service;

import com.loantrackr.dto.request.DisbursementRequest;
import com.loantrackr.dto.request.LoanApplicationRequest;
import com.loantrackr.dto.request.PaymentGatewayRequest;
import com.loantrackr.dto.request.PaymentRequest;
import com.loantrackr.dto.response.*;
import com.loantrackr.enums.LoanRepaymentStatus;
import com.loantrackr.enums.LoanStatus;
import com.loantrackr.enums.PaymentStatus;
import com.loantrackr.enums.Role;
import com.loantrackr.exception.OperationNotAllowedException;
import com.loantrackr.exception.UnauthorizedException;
import com.loantrackr.exception.UserNotFoundException;
import com.loantrackr.model.*;
import com.loantrackr.repository.*;
import com.loantrackr.util.LoanCalculatorUtil;
import com.loantrackr.util.MockPaymentGateway;
import com.loantrackr.util.SecurityUtils;
import com.loantrackr.util.TenureUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.loantrackr.util.LoanCalculatorUtil.*;

@Slf4j
@Service
@AllArgsConstructor
public class LoanService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final LoanConfigurationRepository configRepository;
    private final LenderProfileRepository lenderRepository;
    private final LoanPaymentRepository paymentRepository;
    private final MockPaymentGateway mockPaymentGateway;
    private final LenderProfileService lenderService;
    private final LoanRepository loanRepository;
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

    @Transactional
    public LoanApplicationResponse applyLoan(Long lenderId, LoanApplicationRequest request) {
        String userName = SecurityUtils.getCurrentUserName();
        User borrower = userService.getUserByUserName(userName)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!borrower.getRole().equals(Role.BORROWER))
            throw new OperationNotAllowedException("Only Borrowers can apply for loan");

        // One-loan check
        boolean hasActiveLoan = userHasActiveLoan(borrower.getId());

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
        application.setPurpose(request.getPurpose());
        application.setIncomeSource(request.getIncomeSource());
        application.setMonthlyIncome(request.getMonthlyIncome());
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

    public boolean userHasActiveLoan(Long id) {
        boolean hasActiveLoan = loanApplicationRepository.existsByUserIdAndStatusIn(
                id,
                java.util.List.of(LoanStatus.PENDING, LoanStatus.APPROVED, LoanStatus.DISBURSED)
        );
        if (hasActiveLoan) throw new OperationNotAllowedException("Borrower already has an active loan.");

        return false;
    }

    public boolean withdrawLoan() {
        String userName = SecurityUtils.getCurrentUserName();
        User user = userService.getUserByUserName(userName)
                .orElseThrow(() -> new UserNotFoundException("User not found for username: " + userName));

        if (!Role.BORROWER.equals(user.getRole())) {
            throw new OperationNotAllowedException("Only Borrowers can withdraw loan applications");
        }

        List<LoanApplication> pendingApplications = loanApplicationRepository.findByUserIdAndStatusIn(
                user.getId(), Collections.singletonList(LoanStatus.PENDING)
        );

        if (pendingApplications.isEmpty()) {
            throw new IllegalStateException("No pending loan applications to withdraw");
        }

        LoanApplication applicationToWithdraw = pendingApplications.getFirst();

        applicationToWithdraw.setStatus(LoanStatus.WITHDRAWN);
        loanApplicationRepository.save(applicationToWithdraw);

        return true;
    }

    public List<LoanApplicationResponse> getMyLoanApplications() {
        String username = SecurityUtils.getCurrentUserName();
        User user = userService.getUserByUserName(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (!user.getRole().equals(Role.BORROWER)) {
            throw new UnauthorizedException("Only borrowers can view loan applications");
        }

        return loanApplicationRepository.findByUserId(user.getId()).stream()
                .map(application -> LoanApplicationResponse.builder()
                        .applicationId(application.getId())
                        .loanAmount(application.getLoanRequested())
                        .tenure(application.getTenure())
                        .emi(application.getEmiAmount())
                        .interestRate(application.getInterestRate())
                        .processingFee(application.getProcessingFee())
                        .status(application.getStatus())
                        .lenderName(application.getLender().getOrganizationName())
                        .appliedAt(application.getAppliedAt())
                        .build())
                .toList();
    }


    @Transactional
    public LoanApplicationResponse approveLoan(Long applicationId) {
        LoanApplication loan = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid application ID"));

        if (!loan.getStatus().equals(LoanStatus.PENDING)) {
            throw new IllegalStateException("Loan cannot be approved in current state: " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.APPROVED);
        loanApplicationRepository.save(loan);

        return LoanApplicationResponse.builder()
                .applicationId(loan.getId())
                .loanAmount(loan.getLoanRequested())
                .appliedAt(loan.getAppliedAt())
                .emi(loan.getEmiAmount())
                .interestRate(loan.getInterestRate())
                .processingFee(loan.getProcessingFee())
                .lenderName(loan.getLender().getOrganizationName())
                .status(loan.getStatus())
                .tenure(loan.getTenure())
                .purpose(loan.getPurpose())
                .build();
    }


    @Transactional
    public LoanApplicationResponse rejectLoan(Long applicationId) {
        LoanApplication loan = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid application ID"));

        if (!loan.getStatus().equals(LoanStatus.PENDING)) {
            throw new IllegalStateException("Loan cannot be approved in current state: " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.REJECTED);
        loanApplicationRepository.save(loan);

        return LoanApplicationResponse.builder()
                .applicationId(loan.getId())
                .loanAmount(loan.getLoanRequested())
                .appliedAt(loan.getAppliedAt())
                .emi(loan.getEmiAmount())
                .interestRate(loan.getInterestRate())
                .processingFee(loan.getProcessingFee())
                .lenderName(loan.getLender().getOrganizationName())
                .status(loan.getStatus())
                .tenure(loan.getTenure())
                .purpose(loan.getPurpose())
                .build();
    }

    //Disbursement

    public LoanDisbursementResponse disburseLoan(Long loanApplicationId) {

        LoanApplication application = loanApplicationRepository.findById(loanApplicationId)
                .orElseThrow(() -> new NoSuchElementException("Loan application not found"));

        if (application.getStatus() != LoanStatus.APPROVED) {
            throw new UnauthorizedException("Loan application is not approved");
        }


        BigDecimal principal = application.getLoanRequested();
        BigDecimal annualRate = application.getInterestRate();
        int tenureMonths = application.getTenure();

        BigDecimal emiAmount = LoanCalculatorUtil.calculateEMI(principal, annualRate, tenureMonths);
        BigDecimal totalAmount = LoanCalculatorUtil.calculateTotalPayable(emiAmount, tenureMonths);
        BigDecimal totalInterest = LoanCalculatorUtil.calculateTotalInterest(emiAmount, principal, tenureMonths);


        Loan loan = Loan.builder()
                .loanApplication(application)
                .borrower(application.getUser())
                .lender(application.getLender())
                .principalAmount(principal)
                .totalAmountToRepay(totalAmount)
                .remainingAmount(totalAmount)
                .totalInterestAmount(totalInterest)
                .totalInstallments(tenureMonths)
                .nextDueDate(calculateFirstDueDate())
                .build();

        loan = loanRepository.save(loan);

        generateEMISchedule(loan, emiAmount, annualRate, tenureMonths);

        DisbursementResponse disbursementResponse = processDisbursement(loan);

        application.setStatus(LoanStatus.DISBURSED);
        application.setLoan(loan);
        loanApplicationRepository.save(application);

        log.info("Loan disbursed successfully for application ID: {}", loanApplicationId);

        return LoanDisbursementResponse.builder()
                .loanId(loan.getId())
                .disbursedAmount(principal)
                .emiAmount(emiAmount)
                .totalAmount(totalAmount)
                .totalInterest(totalInterest)
                .firstDueDate(loan.getNextDueDate())
                .disbursementTransactionId(disbursementResponse.getTransactionId())
                .message("Loan disbursed successfully")
                .build();
    }

    private void generateEMISchedule(Loan loan, BigDecimal emiAmount, BigDecimal annualRate, int tenureMonths) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal remainingBalance = loan.getPrincipalAmount();
        LocalDate dueDate = loan.getNextDueDate();

        List<LoanRepaymentSchedule> schedules = new ArrayList<>();

        for (int i = 1; i <= tenureMonths; i++) {
            BigDecimal interestAmount = remainingBalance.multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal principalAmount = emiAmount.subtract(interestAmount);

            if (i == tenureMonths) {
                principalAmount = remainingBalance;
                emiAmount = principalAmount.add(interestAmount);
            }

            LoanRepaymentSchedule schedule = LoanRepaymentSchedule.builder()
                    .loan(loan)
                    .installmentNumber(i)
                    .emiAmount(emiAmount)
                    .principalAmount(principalAmount)
                    .interestAmount(interestAmount)
                    .dueDate(dueDate)
                    .build();

            schedules.add(schedule);

            remainingBalance = remainingBalance.subtract(principalAmount);
            dueDate = dueDate.plusMonths(1);
        }

        scheduleRepository.saveAll(schedules);
    }

    private LocalDate calculateFirstDueDate() {
        return LocalDate.now().plusMonths(1);
    }

    private DisbursementResponse processDisbursement(Loan loan) {
        // Mock disbursement to borrower's account
        DisbursementRequest request = DisbursementRequest.builder()
                .loanId(loan.getId())
                .borrowerAccountNumber(loan.getBorrower().getId().toString()) // Mock
                .amount(loan.getPrincipalAmount())
                .build();

        return mockPaymentGateway.processDisbursement(request);
    }

    //Repayment

    public PaymentResponse makePayment(Long loanId, PaymentRequest request) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Loan not found"));

        if (loan.getStatus() != LoanStatus.DISBURSED) {
            throw new OperationNotAllowedException("Loan is not active for payments");
        }

        LoanRepaymentSchedule nextInstallment = scheduleRepository
                .findByLoanAndStatus(loan, LoanRepaymentStatus.PENDING)
                .stream()
                .min(Comparator.comparing(LoanRepaymentSchedule::getInstallmentNumber))
                .orElseThrow(() -> new OperationNotAllowedException("No pending installments found"));

        calculateAndUpdateLateFee(nextInstallment);

        BigDecimal totalAmountDue = nextInstallment.getTotalAmountDue();
        if (request.getAmount().compareTo(totalAmountDue) != 0) {
            throw new OperationNotAllowedException("Payment amount must be exactly " + totalAmountDue);
        }

        PaymentGatewayResponse gatewayResponse = mockPaymentGateway.processPayment(
                PaymentGatewayRequest.builder()
                        .amount(request.getAmount())
                        .paymentMethod(request.getPaymentMethod())
                        .loanId(loanId)
                        .installmentNumber(nextInstallment.getInstallmentNumber())
                        .build()
        );

        LoanPayment payment = LoanPayment.builder()
                .loan(loan)
                .repaymentSchedule(nextInstallment)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(gatewayResponse.getStatus())
                .transactionId(generateTransactionId())
                .gatewayTransactionId(gatewayResponse.getTransactionId())
                .failureReason(gatewayResponse.getFailureReason())
                .build();

        if (gatewayResponse.getStatus() == PaymentStatus.SUCCESS) {
            payment.setPaidAt(LocalDateTime.now());
            updateInstallmentAndLoan(nextInstallment, loan, request.getAmount());
        }

        paymentRepository.save(payment);

        log.info("Payment processed for loan ID: {}, installment: {}, status: {}",
                loanId, nextInstallment.getInstallmentNumber(), payment.getStatus());

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .transactionId(payment.getTransactionId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .installmentNumber(nextInstallment.getInstallmentNumber())
                .remainingAmount(loan.getRemainingAmount())
                .nextDueDate(loan.getNextDueDate())
                .message(getPaymentMessage(payment.getStatus()))
                .build();
    }

    private void calculateAndUpdateLateFee(LoanRepaymentSchedule installment) {
        if (installment.isOverdue() && installment.getLateFee().compareTo(BigDecimal.ZERO) == 0) {
            LoanConfiguration config = configRepository.findByActive(true);
            if (config == null) {
                config = LoanConfiguration.builder().lateFeeAmount(new BigDecimal("500")).build();
            }

            installment.setLateFee(config.getLateFeeAmount());
            scheduleRepository.save(installment);
        }
    }

    private void updateInstallmentAndLoan(LoanRepaymentSchedule installment, Loan loan, BigDecimal paidAmount) {
        installment.setPaidDate(LocalDate.now());
        installment.setTotalAmountPaid(paidAmount);
        installment.setStatus(installment.isOverdue() ?
                LoanRepaymentStatus.LATE_PAID : LoanRepaymentStatus.PAID);

        loan.setRemainingAmount(loan.getRemainingAmount().subtract(installment.getEmiAmount()));
        loan.setPaidInstallments(loan.getPaidInstallments() + 1);

        if (!loan.isFullyRepaid()) {
            LoanRepaymentSchedule nextInstallment = scheduleRepository
                    .findByLoanAndStatus(loan, LoanRepaymentStatus.PENDING)
                    .stream()
                    .min(Comparator.comparing(LoanRepaymentSchedule::getInstallmentNumber))
                    .orElse(null);

            loan.setNextDueDate(nextInstallment != null ? nextInstallment.getDueDate() : null);
        }

        scheduleRepository.save(installment);
        loanRepository.save(loan);
    }

    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + "_" +
                ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private String getPaymentMessage(PaymentStatus status) {
        return switch (status) {
            case SUCCESS -> "Payment processed successfully";
            case FAILED -> "Payment failed. Please try again";
            case PENDING -> "Payment is being processed";
            case CANCELLED -> "Payment was cancelled";
        };
    }

    public List<LoanRepaymentSchedule> getPaymentSchedule(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Loan not found"));

        return scheduleRepository.findByLoanOrderByInstallmentNumber(loan);
    }

    public List<LoanPayment> getPaymentHistory(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Loan not found"));

        return paymentRepository.findByLoanOrderByCreatedAtDesc(loan);
    }
}
