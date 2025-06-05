package com.TravelShare.controller;

import com.TravelShare.dto.request.SettlementCreationRequest;
import com.TravelShare.dto.request.SettlementUpdateRequest;
import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.BalanceResponse;
import com.TravelShare.dto.response.SettlementResponse;
import com.TravelShare.entity.Group;
import com.TravelShare.entity.Settlement;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.repository.GroupRepository;
import com.TravelShare.repository.SettlementRepository;
import com.TravelShare.service.SettlementService;
import com.TravelShare.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/settlement")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SettlementController {

    SettlementService settlementService;
    SettlementRepository settlementRepository;
    GroupRepository groupRepository;
    VnPayService vnPayService;

    @GetMapping("/group/{groupId}/balances")
    @ResponseBody
    public ApiResponse<List<BalanceResponse>> getTripBalances(@PathVariable Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));

        Map<Long, BigDecimal> balances = settlementService.calculateBalances(group);

        return ApiResponse.<List<BalanceResponse>>builder()
                .result(settlementService.convertToBalanceResponse(group, balances))
                .build();
    }

    @GetMapping("/group/{groupId}/suggested")
    @ResponseBody
    public ApiResponse<List<SettlementResponse>> getSuggestedSettlements(@PathVariable Long groupId, @RequestParam(name = "userOnly", required = false, defaultValue = "false") boolean userOnly,  HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        List<SettlementResponse> suggestions;

        if (userOnly) {
            // Lấy username từ token (sub)
            String username = principal.getName();

            suggestions = settlementService.suggestSettlements(groupId, username);
        } else {
            suggestions = settlementService.suggestSettlements(groupId);
        }
        return ApiResponse.<List<SettlementResponse>>builder()
                .result(suggestions)
                .build();
    }

    @PostMapping
    @ResponseBody
    public ApiResponse<SettlementResponse> createSettlement(
            @Valid @RequestBody SettlementCreationRequest request) {
        return ApiResponse.<SettlementResponse>builder()
                .result(settlementService.createSettlement(request))
                .build();
    }

    // ✅ 4. Xác nhận thanh toán (Confirm Settlement)
    @PatchMapping("/{settlementId}/confirm")
    @ResponseBody
    public ApiResponse<SettlementResponse> updateSettlementStatus(
            @PathVariable Long settlementId,
            @RequestBody SettlementUpdateRequest request) {
        return ApiResponse.<SettlementResponse>builder()
                .result(settlementService.updateSettlementStatus(settlementId, request))
                .build();
    }

    @GetMapping("/group/{groupId}")
    @ResponseBody
    public ApiResponse<List<SettlementResponse>> getGroupSettlements(@PathVariable Long groupId) {
        return ApiResponse.<List<SettlementResponse>>builder()
                .result(settlementService.getGroupSettlements(groupId))
                .build();
    }

    @PostMapping("vnpay/create")
    @ResponseBody
    public ApiResponse<?> createVnPaySettlement(@RequestBody SettlementCreationRequest request) {
        SettlementResponse settlementResponse = settlementService.createSettlement(request);
        Settlement settlement = settlementRepository.findById(settlementResponse.getId())
                .orElseThrow(() -> new AppException(ErrorCode.SETTLEMENT_NOT_FOUND));
        String paymentUrl = vnPayService.createPaymentUrl(settlement);
        Map<String, Object> result = new HashMap<>();
        result.put("paymentUrl", paymentUrl);
        result.put("settlementId", settlement.getId());
        return ApiResponse.builder()
                .result(result)
                .build();
    }

    @GetMapping("vnpay/callback")
    public String vnPayReturn(HttpServletRequest request, Model model) {
        String queryString = request.getQueryString();
        Map<String, String> params = new LinkedHashMap<>();
        if (queryString != null) {
            for (String param : queryString.split("&")) {
                int idx = param.indexOf("=");
                if (idx > 0) {
                    String key = param.substring(0, idx);
                    String value = param.substring(idx + 1); // giữ nguyên dấu +
                    params.put(key, value);
                }
            }
        }
        boolean valid = vnPayService.validateVnPayCallback(params);
        Long settlementId = Long.valueOf(params.get("vnp_TxnRef"));
        String vnp_TransactionStatus = params.get("vnp_TransactionStatus");
        String vnp_TransactionNo = params.get("vnp_TransactionNo");

        // Lấy thông tin chi tiết settlement
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElse(null);
        if (settlement != null) {
            model.addAttribute("from", settlement.getFromParticipant().getName());
            model.addAttribute("to", settlement.getToParticipant().getName());
            model.addAttribute("amount", settlement.getAmount());
            model.addAttribute("currency", settlement.getCurrency().getCode());
            model.addAttribute("description", settlement.getDescription());
        }
        model.addAttribute("transactionNo", vnp_TransactionNo);
        model.addAttribute("status", valid && "00".equals(vnp_TransactionStatus) ? "Thành công" : "Thất bại");

        if (valid && "00".equals(vnp_TransactionStatus)) {
            settlementService.updateSettlementStatusVnPay(settlementId, Settlement.SettlementStatus.COMPLETED, vnp_TransactionNo);
            return "vnpay_success";
        } else {
            settlementService.updateSettlementStatusVnPay(settlementId, Settlement.SettlementStatus.FAILED, vnp_TransactionNo);
            return "vnpay_fail";
        }
    }

    @GetMapping("vnpay/ipn")
    public ResponseEntity<String> vnPayIpn(@RequestParam Map<String, String> params) {
        boolean valid = vnPayService.validateVnPayCallback(params);
        Long settlementId = Long.valueOf(params.get("vnp_TxnRef"));
        String vnp_TransactionStatus = params.get("vnp_TransactionStatus");
        String vnp_TransactionNo = params.get("vnp_TransactionNo");

        if (valid && "00".equals(vnp_TransactionStatus)) {
            settlementService.updateSettlementStatusVnPay(settlementId, Settlement.SettlementStatus.COMPLETED, vnp_TransactionNo);
            return ResponseEntity.ok("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}");
        } else {
            settlementService.updateSettlementStatusVnPay(settlementId, Settlement.SettlementStatus.FAILED, vnp_TransactionNo);
            return ResponseEntity.ok("{\"RspCode\":\"97\",\"Message\":\"Invalid signature\"}");
        }
    }
}

