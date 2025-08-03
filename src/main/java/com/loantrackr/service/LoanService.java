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


    public List<LenderSummaryResponse> getAllActiveLenderResponses() {
        log.info("Fetching all lenders");
        try {
            List<LenderSummaryResponse> lenders = lenderRepository.findAll().stream()
                    .filter(lender -> lender.isVerified() && lender.getUser().isActive() && lender.getUser().isVerified())
                    .map(this::mapToSummary).toList();
            log.info("Successfully retrieved {} lenders", lenders.size());
            return lenders;
        } catch (Exception e) {
            log.error("Error fetching all lenders", e);
            throw e;
        }
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
        log.info("Generating EMI preview for lender ID: {}, principal: {}, tenure: {} months", id, principal, tenure);

        try {
            LenderProfile lenderById = lenderService.getLenderById(id);

            if (!lenderById.isVerified() || !lenderById.getUser().isVerified() || !lenderById.getUser().isActive()) {
                log.warn("EMI preview attempt for unverified/inactive lender ID: {}", id);
                throw new UnauthorizedException("Lender with id: " + id + " is not verified or isn't active");
            }

            boolean tenureSupported = TenureUtils.isTenureSupported(lenderById.getSupportedTenures(), tenure);
            if (!tenureSupported) {
                log.warn("Unsupported tenure {} requested for lender ID: {}", tenure, id);
                throw new OperationNotAllowedException("The lender doesn't support given tenure :" + tenure);
            }

            EmiPreview preview = new EmiPreview();
            BigDecimal emi = calculateEMI(principal, lenderById.getInterestRate(), tenure);
            preview.setEmi(emi);
            preview.setTotalPayable(calculateTotalPayable(emi, tenure));
            preview.setTotalInterest(calculateTotalInterest(emi, principal, tenure));
            preview.setOrganization(lenderById.getOrganizationName());
            preview.setProcessingFee(lenderById.getProcessingFee());

            log.info("EMI preview generated successfully for lender: {}, EMI: {}", lenderById.getOrganizationName(), emi);
            return preview;

        } catch (UnauthorizedException | OperationNotAllowedException e) {
            log.error("Business rule violation during EMI preview for lender ID: {} - {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error generating EMI preview for lender ID: {}", id, e);
            throw e;
        }
    }

    @Transactional
    public LoanApplicationResponse applyLoan(Long lenderId, LoanApplicationRequest request) {
        String userName = SecurityUtils.getCurrentUserName();
        log.info("Loan application initiated by user: {} for lender ID: {}, amount: {}", userName, lenderId, request.getLoanAmount());

        try {
            User borrower = userService.getUserByUserName(userName).orElseThrow(() -> new UserNotFoundException("User not found"));

            if (!borrower.getRole().equals(Role.BORROWER)) {
                log.warn("Non-borrower user {} attempted to apply for loan", userName);
                throw new OperationNotAllowedException("Only Borrowers can apply for loan");
            }

            // One-loan check
            boolean hasActiveLoan = userHasActiveLoan(borrower.getId());

            LenderProfile lender = lenderService.getLenderById(lenderId);

            if (!lender.isVerified()) {
                log.warn("Loan application attempted for unverified lender ID: {}", lenderId);
                throw new UnauthorizedException("Lender is not verified");
            }

            if (!TenureUtils.isTenureSupported(lender.getSupportedTenures(), request.getTenureInMonths())) {
                log.warn("Unsupported tenure {} requested for lender: {}", request.getTenureInMonths(), lender.getOrganizationName());
                throw new OperationNotAllowedException("Selected tenure is not supported by this lender");
            }

            // Calculate EMI
            BigDecimal emi = LoanCalculatorUtil.calculateEMI(request.getLoanAmount(), lender.getInterestRate(), request.getTenureInMonths());

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

            log.info("Loan application submitted successfully - ID: {}, User: {}, Lender: {}, Amount: {}", application.getId(), userName, lender.getOrganizationName(), request.getLoanAmount());

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

        } catch (UserNotFoundException | UnauthorizedException | OperationNotAllowedException e) {
            log.error("Loan application failed for user: {} - {}", userName, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during loan application for user: {}", userName, e);
            throw e;
        }
    }

    public boolean userHasActiveLoan(Long id) {
        log.info("Checking active loan status for user ID: {}", id);

        boolean hasActiveLoan = loanApplicationRepository.existsByUserIdAndStatusIn(id, java.util.List.of(LoanStatus.PENDING, LoanStatus.APPROVED, LoanStatus.DISBURSED));

        if (hasActiveLoan) {
            log.warn("User ID: {} already has an active loan", id);
            throw new OperationNotAllowedException("Borrower already has an active loan.");
        }

        log.info("User ID: {} has no active loans", id);
        return false;
    }

    public boolean withdrawLoan() {
        String userName = SecurityUtils.getCurrentUserName();
        log.info("Loan withdrawal initiated by user: {}", userName);

        try {
            User user = userService.getUserByUserName(userName).orElseThrow(() -> new UserNotFoundException("User not found for username: " + userName));

            if (!Role.BORROWER.equals(user.getRole())) {
                log.warn("Non-borrower user {} attempted to withdraw loan", userName);
                throw new OperationNotAllowedException("Only Borrowers can withdraw loan applications");
            }

            List<LoanApplication> pendingApplications = loanApplicationRepository.findByUserIdAndStatusIn(user.getId(), Collections.singletonList(LoanStatus.PENDING));

            if (pendingApplications.isEmpty()) {
                log.warn("No pending applications found for withdrawal - User: {}", userName);
                throw new IllegalStateException("No pending loan applications to withdraw");
            }

            LoanApplication applicationToWithdraw = pendingApplications.getFirst();
            applicationToWithdraw.setStatus(LoanStatus.WITHDRAWN);
            loanApplicationRepository.save(applicationToWithdraw);

            log.info("Loan application withdrawn successfully - Application ID: {}, User: {}", applicationToWithdraw.getId(), userName);
            return true;

        } catch (UserNotFoundException | OperationNotAllowedException | IllegalStateException e) {
            log.error("Loan withdrawal failed for user: {} - {}", userName, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during loan withdrawal for user: {}", userName, e);
            throw e;
        }
    }

    public List<LoanApplicationResponse> getMyLoanApplications() {
        String username = SecurityUtils.getCurrentUserName();
        log.info("Fetching loan applications for user: {}", username);

        try {
            User user = userService.getUserByUserName(username).orElseThrow(() -> new UserNotFoundException("User not found"));

            if (!user.getRole().equals(Role.BORROWER)) {
                log.warn("Non-borrower user {} attempted to view loan applications", username);
                throw new UnauthorizedException("Only borrowers can view loan applications");
            }

            List<LoanApplicationResponse> applications = loanApplicationRepository.findByUserId(user.getId())
                    .stream()
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

            log.info("Retrieved {} loan applications for user: {}", applications.size(), username);
            return applications;

        } catch (UserNotFoundException | UnauthorizedException e) {
            log.error("Failed to fetch loan applications for user: {} - {}", username, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching loan applications for user: {}", username, e);
            throw e;
        }
    }

    @Transactional
    public LoanApplicationResponse approveLoan(Long applicationId) {
        log.info("Loan approval initiated for application ID: {}", applicationId);

        try {
            LoanApplication loan = loanApplicationRepository.findById(applicationId).orElseThrow(() -> new IllegalArgumentException("Invalid application ID"));

            if (!loan.getStatus().equals(LoanStatus.PENDING)) {
                log.warn("Approval attempted for non-pending loan - Application ID: {}, Current status: {}", applicationId, loan.getStatus());
                throw new IllegalStateException("Loan cannot be approved in current state: " + loan.getStatus());
            }

            loan.setStatus(LoanStatus.APPROVED);
            loanApplicationRepository.save(loan);

            log.info("Loan approved successfully - Application ID: {}, Borrower: {}, Amount: {}", applicationId, loan.getUser().getUsername(), loan.getLoanRequested());

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

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Loan approval failed for application ID: {} - {}", applicationId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during loan approval for application ID: {}", applicationId, e);
            throw e;
        }
    }

    @Transactional
    public LoanApplicationResponse rejectLoan(Long applicationId) {
        log.info("Loan rejection initiated for application ID: {}", applicationId);

        try {
            LoanApplication loan = loanApplicationRepository.findById(applicationId).orElseThrow(() -> new IllegalArgumentException("Invalid application ID"));

            if (!loan.getStatus().equals(LoanStatus.PENDING)) {
                log.warn("Rejection attempted for non-pending loan - Application ID: {}, Current status: {}", applicationId, loan.getStatus());
                throw new IllegalStateException("Loan cannot be rejected in current state: " + loan.getStatus());
            }

            loan.setStatus(LoanStatus.REJECTED);
            loanApplicationRepository.save(loan);

            log.info("Loan rejected - Application ID: {}, Borrower: {}, Amount: {}", applicationId, loan.getUser().getUsername(), loan.getLoanRequested());

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

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Loan rejection failed for application ID: {} - {}", applicationId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during loan rejection for application ID: {}", applicationId, e);
            throw e;
        }
    }

    //Disbursement
    public LoanDisbursementResponse disburseLoan(Long loanApplicationId) {
        log.info("Loan disbursement initiated for application ID: {}", loanApplicationId);

        try {
            LoanApplication application = loanApplicationRepository.findById(loanApplicationId).orElseThrow(() -> new NoSuchElementException("Loan application not found"));

            if (application.getStatus() != LoanStatus.APPROVED) {
                log.warn("Disbursement attempted for non-approved loan - Application ID: {}, Status: {}", loanApplicationId, application.getStatus());
                throw new UnauthorizedException("Loan application is not approved");
            }

            BigDecimal principal = application.getLoanRequested();
            BigDecimal annualRate = application.getInterestRate();
            int tenureMonths = application.getTenure();

            log.info("Processing disbursement - Amount: {}, Rate: {}%, Tenure: {} months", principal, annualRate, tenureMonths);

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
            log.info("Loan entity created with ID: {}", loan.getId());

            generateEMISchedule(loan, emiAmount, annualRate, tenureMonths);
            log.info("EMI schedule generated for {} installments", tenureMonths);

            DisbursementResponse disbursementResponse = processDisbursement(loan);
            log.info("Disbursement processed via payment gateway - Transaction ID: {}", disbursementResponse.getTransactionId());

            application.setStatus(LoanStatus.DISBURSED);
            application.setLoan(loan);
            loanApplicationRepository.save(application);

            log.info("Loan disbursed successfully - Loan ID: {}, Amount: {}, Borrower: {}", loan.getId(), principal, application.getUser().getUsername());

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

        } catch (NoSuchElementException | UnauthorizedException e) {
            log.error("Loan disbursement failed for application ID: {} - {}", loanApplicationId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during loan disbursement for application ID: {}", loanApplicationId, e);
            throw e;
        }
    }

    private void generateEMISchedule(Loan loan, BigDecimal emiAmount, BigDecimal annualRate, int tenureMonths) {
        log.info("Generating EMI schedule for loan ID: {} with {} installments", loan.getId(), tenureMonths);

        try {
            BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
            BigDecimal remainingBalance = loan.getPrincipalAmount();
            LocalDate dueDate = loan.getNextDueDate();

            List<LoanRepaymentSchedule> schedules = new ArrayList<>();

            for (int i = 1; i <= tenureMonths; i++) {
                BigDecimal interestAmount = remainingBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

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
            log.info("EMI schedule saved successfully for loan ID: {}", loan.getId());

        } catch (Exception e) {
            log.error("Error generating EMI schedule for loan ID: {}", loan.getId(), e);
            throw e;
        }
    }

    private LocalDate calculateFirstDueDate() {
        return LocalDate.now().plusMonths(1);
    }

    private DisbursementResponse processDisbursement(Loan loan) {
        log.info("Processing disbursement via payment gateway for loan ID: {}", loan.getId());

        try {
            DisbursementRequest request = DisbursementRequest.builder()
                    .loanId(loan.getId())
                    .borrowerAccountNumber(loan.getBorrower().getId().toString()) // Mock
                    .amount(loan.getPrincipalAmount())
                    .build();

            DisbursementResponse response = mockPaymentGateway.processDisbursement(request);
            log.info("Payment gateway disbursement response - Status: {}, Transaction ID: {}", response.getStatus(), response.getTransactionId());

            return response;
        } catch (Exception e) {
            log.error("Payment gateway disbursement failed for loan ID: {}", loan.getId(), e);
            throw e;
        }
    }

    //Repayment
    public PaymentResponse makePayment(Long loanId, PaymentRequest request) {
        log.info("Payment initiated for loan ID: {}, amount: {}", loanId, request.getAmount());

        try {
            Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new NoSuchElementException("Loan not found"));

            if (loan.getStatus() != LoanStatus.DISBURSED) {
                log.warn("Payment attempted for inactive loan - Loan ID: {}, Status: {}", loanId, loan.getStatus());
                throw new OperationNotAllowedException("Loan is not active for payments");
            }

            LoanRepaymentSchedule nextInstallment = scheduleRepository.findByLoanAndStatus(loan, LoanRepaymentStatus.PENDING).stream().min(Comparator.comparing(LoanRepaymentSchedule::getInstallmentNumber)).orElseThrow(() -> new OperationNotAllowedException("No pending installments found"));

            log.info("Processing payment for installment {} of loan ID: {}", nextInstallment.getInstallmentNumber(), loanId);

            calculateAndUpdateLateFee(nextInstallment);

            BigDecimal totalAmountDue = nextInstallment.getTotalAmountDue();
            if (request.getAmount().compareTo(totalAmountDue) != 0) {
                log.warn("Payment amount mismatch - Expected: {}, Received: {} for loan ID: {}", totalAmountDue, request.getAmount(), loanId);
                throw new OperationNotAllowedException("Payment amount must be exactly " + totalAmountDue);
            }

            PaymentGatewayResponse gatewayResponse = mockPaymentGateway.processPayment(PaymentGatewayRequest.builder()
                    .amount(request.getAmount())
                    .paymentMethod(request.getPaymentMethod())
                    .loanId(loanId)
                    .installmentNumber(nextInstallment.getInstallmentNumber())
                    .build());

            log.info("Payment gateway response - Status: {}, Gateway Transaction ID: {}", gatewayResponse.getStatus(), gatewayResponse.getTransactionId());

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
                log.info("Payment successful - Loan ID: {}, Installment: {}, Remaining amount: {}", loanId, nextInstallment.getInstallmentNumber(), loan.getRemainingAmount());
            } else {
                log.warn("Payment failed - Loan ID: {}, Installment: {}, Reason: {}", loanId, nextInstallment.getInstallmentNumber(), gatewayResponse.getFailureReason());
            }

            paymentRepository.save(payment);

            log.info("Payment record saved - Payment ID: {}, Status: {}", payment.getId(), payment.getStatus());

            return PaymentResponse.builder().paymentId(payment.getId()).transactionId(payment.getTransactionId()).status(payment.getStatus()).amount(payment.getAmount()).installmentNumber(nextInstallment.getInstallmentNumber()).remainingAmount(loan.getRemainingAmount()).nextDueDate(loan.getNextDueDate()).message(getPaymentMessage(payment.getStatus())).build();

        } catch (NoSuchElementException | OperationNotAllowedException e) {
            log.error("Payment failed for loan ID: {} - {}", loanId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during payment for loan ID: {}", loanId, e);
            throw e;
        }
    }

    private void calculateAndUpdateLateFee(LoanRepaymentSchedule installment) {
        if (installment.isOverdue() && installment.getLateFee().compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Calculating late fee for overdue installment - Loan ID: {}, Installment: {}", installment.getLoan().getId(), installment.getInstallmentNumber());

            try {
                LoanConfiguration config = configRepository.findByActive(true);
                if (config == null) {
                    log.warn("No active loan configuration found, using default late fee");
                    config = LoanConfiguration.builder().lateFeeAmount(new BigDecimal("500")).build();
                }

                installment.setLateFee(config.getLateFeeAmount());
                scheduleRepository.save(installment);

                log.info("Late fee applied - Amount: {}, Loan ID: {}, Installment: {}", config.getLateFeeAmount(), installment.getLoan().getId(), installment.getInstallmentNumber());

            } catch (Exception e) {
                log.error("Error calculating late fee for installment - Loan ID: {}, Installment: {}", installment.getLoan().getId(), installment.getInstallmentNumber(), e);
                throw e;
            }
        }
    }

    private void updateInstallmentAndLoan(LoanRepaymentSchedule installment, Loan loan, BigDecimal paidAmount) {
        log.info("Updating installment and loan status - Loan ID: {}, Installment: {}", loan.getId(), installment.getInstallmentNumber());

        try {
            installment.setPaidDate(LocalDate.now());
            installment.setTotalAmountPaid(paidAmount);
            installment.setStatus(installment.isOverdue() ? LoanRepaymentStatus.LATE_PAID : LoanRepaymentStatus.PAID);

            loan.setRemainingAmount(loan.getRemainingAmount().subtract(installment.getEmiAmount()));
            loan.setPaidInstallments(loan.getPaidInstallments() + 1);

            if (!loan.isFullyRepaid()) {
                LoanRepaymentSchedule nextInstallment = scheduleRepository.findByLoanAndStatus(loan, LoanRepaymentStatus.PENDING).stream().min(Comparator.comparing(LoanRepaymentSchedule::getInstallmentNumber)).orElse(null);

                loan.setNextDueDate(nextInstallment != null ? nextInstallment.getDueDate() : null);
            } else {
                log.info("Loan fully repaid - Loan ID: {}", loan.getId());
                loan.setNextDueDate(null);
            }

            scheduleRepository.save(installment);
            loanRepository.save(loan);

            log.info("Loan and installment updated successfully - Remaining installments: {}", loan.getTotalInstallments() - loan.getPaidInstallments());

        } catch (Exception e) {
            log.error("Error updating installment and loan - Loan ID: {}, Installment: {}", loan.getId(), installment.getInstallmentNumber(), e);
            throw e;
        }
    }

    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(1000, 9999);
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
        log.info("Fetching payment schedule for loan ID: {}", loanId);

        try {
            Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new NoSuchElementException("Loan not found"));

            List<LoanRepaymentSchedule> schedule = scheduleRepository.findByLoanOrderByInstallmentNumber(loan);
            log.info("Retrieved payment schedule with {} installments for loan ID: {}", schedule.size(), loanId);

            return schedule;
        } catch (NoSuchElementException e) {
            log.error("Payment schedule fetch failed - Loan not found: {}", loanId);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching payment schedule for loan ID: {}", loanId, e);
            throw e;
        }
    }

    public List<LoanPayment> getPaymentHistory(Long loanId) {
        log.info("Fetching payment history for loan ID: {}", loanId);
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new NoSuchElementException("Loan not found"));

        return paymentRepository.findByLoanOrderByCreatedAtDesc(loan);
    }

    public List<LoanApplicationResponseForLender> getMyLoanRequests() {
        String username = SecurityUtils.getCurrentUserName();
        log.info("Fetching loan requests for lender: {}", username);

        try {
            User user = userService.getUserByUserName(username)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            if (!user.getRole().equals(Role.LENDER)) {
                log.warn("Non-lender user {} attempted to view loan requests", username);
                throw new UnauthorizedException("Only lenders can view loan requests");
            }

            LenderProfile lender = lenderService.getLenderById(user.getId());

            List<LoanApplicationResponseForLender> applications = loanApplicationRepository
                    .findByLenderId(lender.getId())
                    .stream()
                    .map(this::mapToLoanApplicationResponseForLender)
                    .toList();

            log.info("Retrieved {} loan requests for lender: {}", applications.size(), username);
            return applications;

        } catch (UserNotFoundException | UnauthorizedException e) {
            log.error("Failed to fetch loan requests for lender: {} - {}", username, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching loan requests for lender: {}", username, e);
            throw e;
        }
    }


    public List<LoanApplicationResponseForLender> getLoanRequestsByStatus(LoanStatus status) {
        String username = SecurityUtils.getCurrentUserName();
        log.info("Fetching {} loan requests for lender: {}", status, username);

        try {
            User user = userService.getUserByUserName(username)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            if (!user.getRole().equals(Role.LENDER)) {
                log.warn("Non-lender user {} attempted to view loan requests", username);
                throw new UnauthorizedException("Only lenders can view loan requests");
            }

            LenderProfile lender = lenderService.getLenderById(user.getId());

            List<LoanApplicationResponseForLender> applications = loanApplicationRepository
                    .findByLenderIdAndStatus(lender.getId(), status)
                    .stream()
                    .map(this::mapToLoanApplicationResponseForLender)
                    .toList();

            log.info("Retrieved {} {} loan requests for lender: {}", applications.size(), status, username);
            return applications;

        } catch (UserNotFoundException | UnauthorizedException e) {
            log.error("Failed to fetch {} loan requests for lender: {} - {}", status, username, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching {} loan requests for lender: {}", status, username, e);
            throw e;
        }
    }


    public List<LoanApplicationResponseForLender> getApprovedLoans() {
        return getLoanRequestsByStatus(LoanStatus.APPROVED);
    }


    public List<LoanApplicationResponseForLender> getRejectedLoans() {
        return getLoanRequestsByStatus(LoanStatus.REJECTED);
    }


    public List<LoanApplicationResponseForLender> getDisbursedLoans() {
        return getLoanRequestsByStatus(LoanStatus.DISBURSED);
    }


    public List<LoanApplicationResponseForLender> getPendingLoans() {
        return getLoanRequestsByStatus(LoanStatus.PENDING);
    }


    public List<LoanDetailsResponse> getCurrentLenderActiveLoans() {
        String username = SecurityUtils.getCurrentUserName();
        log.info("Fetching active loans for lender: {}", username);

        try {
            User user = userService.getUserByUserName(username)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            if (!user.getRole().equals(Role.LENDER)) {
                log.warn("Non-lender user {} attempted to view active loans", username);
                throw new UnauthorizedException("Only lenders can view active loans");
            }

            LenderProfile lender = lenderService.getLenderById(user.getId());

            List<LoanDetailsResponse> loans = loanRepository
                    .findByLenderIdAndStatus(lender.getId(), LoanStatus.DISBURSED)
                    .stream()
                    .map(this::mapToLoanDetailsResponse)
                    .toList();

            log.info("Retrieved {} active loans for lender: {}", loans.size(), username);
            return loans;

        } catch (UserNotFoundException | UnauthorizedException e) {
            log.error("Failed to fetch active loans for lender: {} - {}", username, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching active loans for lender: {}", username, e);
            throw e;
        }
    }

    public List<LoanDetailsResponse> getCompletedLoans() {
        String username = SecurityUtils.getCurrentUserName();
        log.info("Fetching completed loans for lender: {}", username);

        try {
            User user = userService.getUserByUserName(username)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            if (!user.getRole().equals(Role.LENDER)) {
                log.warn("Non-lender user {} attempted to view completed loans", username);
                throw new UnauthorizedException("Only lenders can view completed loans");
            }

            LenderProfile lender = lenderService.getLenderById(user.getId());

            List<LoanDetailsResponse> loans = loanRepository
                    .findByLenderIdAndStatus(lender.getId(), LoanStatus.CLOSED)
                    .stream()
                    .map(this::mapToLoanDetailsResponse)
                    .toList();

            log.info("Retrieved {} completed loans for lender: {}", loans.size(), username);
            return loans;

        } catch (UserNotFoundException | UnauthorizedException e) {
            log.error("Failed to fetch completed loans for lender: {} - {}", username, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching completed loans for lender: {}", username, e);
            throw e;
        }
    }


    public LoanDetailsResponse getLoanById(Long loanId) {
        String username = SecurityUtils.getCurrentUserName();
        log.info("Fetching loan details for loan ID: {} by lender: {}", loanId, username);

        try {
            User user = userService.getUserByUserName(username)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            if (!user.getRole().equals(Role.LENDER)) {
                log.warn("Non-lender user {} attempted to view loan details", username);
                throw new UnauthorizedException("Only lenders can view loan details");
            }

            LenderProfile lender = lenderService.getLenderById(user.getId());

            Loan loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new NoSuchElementException("Loan not found"));

            if (!loan.getLender().getId().equals(lender.getId())) {
                log.warn("Lender {} attempted to access loan {} that doesn't belong to them", username, loanId);
                throw new UnauthorizedException("You can only view loans associated with your organization");
            }

            LoanDetailsResponse response = mapToLoanDetailsResponse(loan);
            log.info("Retrieved loan details for loan ID: {}", loanId);
            return response;

        } catch (UserNotFoundException | UnauthorizedException | NoSuchElementException e) {
            log.error("Failed to fetch loan details for loan ID: {} - {}", loanId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching loan details for loan ID: {}", loanId, e);
            throw e;
        }
    }


    private LoanDetailsResponse mapToLoanDetailsResponse(Loan loan) {
        return LoanDetailsResponse.builder()
                .loanId(loan.getId())
                .borrowerName(loan.getBorrower().getUsername())
                .principalAmount(loan.getPrincipalAmount())
                .totalAmountToRepay(loan.getTotalAmountToRepay())
                .remainingAmount(loan.getRemainingAmount())
                .totalInterestAmount(loan.getTotalInterestAmount())
                .totalInstallments(loan.getTotalInstallments())
                .paidInstallments(loan.getPaidInstallments())
                .nextDueDate(loan.getNextDueDate())
                .status(loan.getStatus())
                .disbursedAt(loan.getDisbursedAt())
                .isFullyRepaid(loan.isFullyRepaid())
                .build();
    }

    private LoanApplicationResponseForLender mapToLoanApplicationResponseForLender(LoanApplication application) {
        return LoanApplicationResponseForLender.builder()
                .applicationId(application.getId())
                .loanAmount(application.getLoanRequested())
                .tenure(application.getTenure())
                .emi(application.getEmiAmount())
                .interestRate(application.getInterestRate())
                .processingFee(application.getProcessingFee())
                .status(application.getStatus())
                .lenderName(application.getLender().getOrganizationName())
                .appliedAt(application.getAppliedAt())
                .purpose(application.getPurpose())
                .borrowerName(application.getUser().getUsername())
                .borrowerEmail(application.getUser().getEmail())
                .monthlyIncome(application.getMonthlyIncome())
                .incomeSource(application.getIncomeSource())
                .build();
    }
}
