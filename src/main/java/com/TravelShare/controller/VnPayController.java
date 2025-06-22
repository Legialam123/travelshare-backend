package com.TravelShare.controller;

import com.TravelShare.dto.request.SettlementCreationRequest;
import com.TravelShare.dto.request.VnPaySettlementRequest;
import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.SettlementResponse;
import com.TravelShare.entity.Settlement;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.repository.RequestRepository;
import com.TravelShare.repository.SettlementRepository;
import com.TravelShare.service.RequestService;
import com.TravelShare.service.SettlementService;
import com.TravelShare.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/vnpay")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class VnPayController {
    VnPayService vnPayService;
    SettlementService settlementService;
    SettlementRepository settlementRepository;
    RequestService requestService;

    @PostMapping("create")
    @ResponseBody
    public ApiResponse<?> createVnPaySettlement(@RequestBody VnPaySettlementRequest request) {
        Settlement settlement;
        if (request.getSettlementId() != null) {
            // Trường hợp thanh toán từ yêu cầu (settlement đã tồn tại)
            settlement = settlementRepository.findById(request.getSettlementId())
                    .orElseThrow(() -> new AppException(ErrorCode.SETTLEMENT_NOT_FOUND));
        } else {
            // Trường hợp chủ động thanh toán (tạo mới settlement)
            SettlementCreationRequest creationRequest = new SettlementCreationRequest(
                    request.getGroupId(),
                    request.getFromParticipantId(),
                    request.getToParticipantId(),
                    request.getAmount(),
                    request.getCurrencyCode(),
                    request.getSettlementMethod(),
                    request.getDescription(),
                    request.getStatus()
            );
            SettlementResponse settlementResponse = settlementService.createSettlement(creationRequest);
            settlement = settlementRepository.findById(settlementResponse.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.SETTLEMENT_NOT_FOUND));
        }
        String paymentUrl = vnPayService.createPaymentUrl(settlement);
        Map<String, Object> result = new HashMap<>();
        result.put("paymentUrl", paymentUrl);
        result.put("settlementId", settlement.getId());
        return ApiResponse.builder()
                .result(result)
                .build();
    }

    @GetMapping("callback")
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
            requestService.acceptPaymentRequestBySettlementId(settlementId);
            return "vnpay_success";
        } else {
            settlementService.updateSettlementStatusVnPay(settlementId, Settlement.SettlementStatus.FAILED, vnp_TransactionNo);
            return "vnpay_fail";
        }
    }

    @GetMapping("ipn")
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
