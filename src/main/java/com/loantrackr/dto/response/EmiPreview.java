package com.loantrackr.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmiPreview {
    public String organization;
    public BigDecimal emi;
    public BigDecimal totalPayable;
    public BigDecimal totalInterest;
    public BigDecimal processingFee;
}
