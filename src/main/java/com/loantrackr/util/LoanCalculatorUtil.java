package com.loantrackr.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class LoanCalculatorUtil {

    /**
     * Calculates EMI (Equated Monthly Installment) for a loan.
     *
     * @param principal      Loan amount
     * @param annualRate     Annual interest rate in percentage (e.g., 14.5)
     * @param tenureInMonths Loan tenure in months
     * @return EMI rounded to 2 decimal places
     */
    public static BigDecimal calculateEMI(BigDecimal principal, BigDecimal annualRate, int tenureInMonths) {
        if (principal == null || annualRate == null || tenureInMonths <= 0) {
            throw new IllegalArgumentException("Invalid input for EMI calculation");
        }

        //[P*R*(1+R)^N]/[(1+R)^N-1]
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12 * 100), 10, RoundingMode.HALF_UP); // r
        BigDecimal onePlusRPowerN = monthlyRate.add(BigDecimal.ONE).pow(tenureInMonths); // (1 + r)^n
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusRPowerN);
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP); // EMI
    }

    /**
     * Calculates total interest payable over the tenure.
     *
     * @param emi            Monthly EMI
     * @param principal      Original loan amount
     * @param tenureInMonths Loan duration in months
     * @return Total interest paid
     */
    public static BigDecimal calculateTotalInterest(BigDecimal emi, BigDecimal principal, int tenureInMonths) {
        BigDecimal totalPayable = emi.multiply(BigDecimal.valueOf(tenureInMonths));
        return totalPayable.subtract(principal).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates total repayment amount (principal + interest)
     *
     * @param emi            Monthly EMI
     * @param tenureInMonths Loan duration in months
     * @return Total payable amount
     */
    public static BigDecimal calculateTotalPayable(BigDecimal emi, int tenureInMonths) {
        return emi.multiply(BigDecimal.valueOf(tenureInMonths)).setScale(2, RoundingMode.HALF_UP);
    }
}

