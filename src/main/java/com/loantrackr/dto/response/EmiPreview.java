package com.loantrackr.dto.response;

import java.math.BigDecimal;

public class EmiPreview {
    public String organization;
    public BigDecimal emi;
    public BigDecimal totalPayable;
    public BigDecimal totalInterest;
    public BigDecimal processingFee;
}
