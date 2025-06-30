package com.example.riskfraud.api;

import com.example.riskfraud.api.dto.TransactionRequest;
import com.example.riskfraud.model.RiskAssessment;
import com.example.riskfraud.model.Transaction;
import com.example.riskfraud.service.RiskAssessmentService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST controller for handling risk assessment requests. */
@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskAssessmentController {

    private final RiskAssessmentService riskAssessmentService;

    /**
     * Performs a comprehensive risk assessment for a transaction.
     *
     * @param request The transaction details
     * @return The risk assessment result
     */
    @PostMapping("/assess")
    public ResponseEntity<RiskAssessment> assessRisk(
            @RequestBody @Valid TransactionRequest request) {
        Transaction transaction = request.toTransaction();
        RiskAssessment assessment = riskAssessmentService.assessRisk(transaction);
        return ResponseEntity.ok(assessment);
    }
}
