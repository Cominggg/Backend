package com.Coming.Backend.policy.controller;

import com.Coming.Backend.policy.dto.PolicyRegisterRequest;
import com.Coming.Backend.policy.dto.PolicyResponse;
import com.Coming.Backend.policy.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Policy")
@RestController
@RequestMapping("/api/admin/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @Operation(summary = "정책 버전 등록")
    @ApiResponse(responseCode = "409", description = "POLICY_VERSION_DUPLICATE")
    @PostMapping
    public ResponseEntity<PolicyResponse> registerPolicy(@Valid @RequestBody PolicyRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.registerPolicy(request));
    }
}
