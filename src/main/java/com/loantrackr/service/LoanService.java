package com.loantrackr.service;

import com.loantrackr.dto.response.EmiPreview;
import com.loantrackr.dto.response.LenderSummaryResponse;
import com.loantrackr.model.LenderProfile;
import com.loantrackr.repository.LenderProfileRepository;
import com.loantrackr.util.TenureUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static com.loantrackr.util.LoanCalculatorUtil.*;

@Service
@AllArgsConstructor
public class LoanService {

    private final LenderProfileRepository lenderRepository;
    private final LenderProfileService lenderService;


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
        EmiPreview preview = new EmiPreview();
        BigDecimal emi = calculateEMI(principal, lenderById.getInterestRate(), tenure);
        preview.setEmi(emi);
        preview.setTotalPayable(calculateTotalPayable(emi, tenure));
        preview.setTotalInterest(calculateTotalInterest(emi, principal, tenure));
        preview.setOrganization(lenderById.getOrganizationName());
        preview.setProcessingFee(lenderById.getProcessingFee());
        return preview;
    }

}
