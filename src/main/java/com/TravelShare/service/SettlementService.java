package com.TravelShare.service;

import com.TravelShare.dto.request.RequestCreationRequest;
import com.TravelShare.dto.request.SettlementCreationRequest;
import com.TravelShare.dto.request.SettlementUpdateRequest;
import com.TravelShare.dto.response.BalanceResponse;
import com.TravelShare.dto.response.SettlementResponse;
import com.TravelShare.entity.*;
import com.TravelShare.entity.Currency;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.mapper.SettlementMapper;
import com.TravelShare.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SettlementService {
    SettlementMapper settlementMapper;
    SettlementRepository settlementRepository;
    GroupRepository groupRepository;
    GroupParticipantRepository participantRepository;
    CurrencyRepository currencyRepository;
    ExpenseRepository expenseRepository;
    UserRepository    userRepository;
    RequestService requestService;

    public List<SettlementResponse> suggestSettlements(Long tripId) {
        return suggestSettlements(tripId, null); // Gọi lại hàm chính với username = null
    }

    public List<SettlementResponse> suggestSettlements(Long groupId, String username) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));

        // Calculate balances using converted amounts for consistency in group currency
        Map<Long, BigDecimal> balances = calculateBalances(group);

        List<Map.Entry<Long, BigDecimal>> debtors = new ArrayList<>();
        List<Map.Entry<Long, BigDecimal>> creditors = new ArrayList<>();

        for (var entry : balances.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(entry);
            } else if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(entry);
            }
        }

        // Sort by amount (largest debts and credits first for efficient settlement)
        debtors.sort((a, b) -> b.getValue().abs().compareTo(a.getValue().abs()));
        creditors.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<SettlementResponse> suggestions = new ArrayList<>();

        int i = 0, j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            Long debtorId = debtors.get(i).getKey();
            Long creditorId = creditors.get(j).getKey();

            BigDecimal debtAmount = debtors.get(i).getValue().abs();
            BigDecimal creditAmount = creditors.get(j).getValue();
            BigDecimal settlementAmount = debtAmount.min(creditAmount);

            GroupParticipant from = participantRepository.findById(debtorId)
                    .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));
            GroupParticipant to = participantRepository.findById(creditorId)
                    .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

            SettlementResponse suggestion = SettlementResponse.builder()
                    .groupId(group.getId())
                    .fromParticipantId(from.getId())
                    .fromParticipantName(from.getName())
                    .toParticipantId(to.getId())
                    .toParticipantName(to.getName())
                    .amount(settlementAmount)
                    .currencyCode(group.getDefaultCurrency().getCode()) // Always use group default currency
                    .description("Gợi ý thanh toán từ hệ thống")
                    .build();

            suggestions.add(suggestion);

            // Update remaining balances
            debtors.get(i).setValue(debtors.get(i).getValue().add(settlementAmount));
            creditors.get(j).setValue(creditors.get(j).getValue().subtract(settlementAmount));

            // Move to next participant if balance is settled (within 0.01 tolerance)
            if (debtors.get(i).getValue().abs().compareTo(new BigDecimal("0.01")) < 0) i++;
            if (creditors.get(j).getValue().compareTo(new BigDecimal("0.01")) < 0) j++;
        }
        if (username != null) {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

            // Tìm participant của user theo email
            GroupParticipant currentParticipant = participantRepository.findByGroupIdAndUserId(groupId, user.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

            Long userParticipantId = currentParticipant.getId();

            suggestions = suggestions.stream()
                    .filter(s -> s.getFromParticipantId().equals(userParticipantId)
                            || s.getToParticipantId().equals(userParticipantId))
                    .collect(Collectors.toList());
        }
        return suggestions;
}

    public Map<Long, BigDecimal> calculateBalances(Group group) {
        Map<Long, BigDecimal> balances = new HashMap<>();

        // Initialize all participant balances to zero
        for (GroupParticipant participant : group.getParticipants()) {
            balances.put(participant.getId(), BigDecimal.ZERO);
        }

        // Process all expenses in the trip
        List<Expense> expenses = expenseRepository.findAllByGroupId(group.getId());

        for (Expense expense : expenses) {
            for (ExpenseSplit split : expense.getSplits()) {
                if (split.getParticipant() == null) continue;

                Long participantId = split.getParticipant().getId();
                BigDecimal splitAmount = split.getAmount();

                // Use convertedAmount for settlement calculations (group currency consistency)
                // All expenses are already converted to group default currency
                BigDecimal expenseAmountInGroupCurrency = expense.getConvertedAmount();

                // If they paid, add the total expense amount to their balance
                if (split.isPayer()) {
                    balances.put(participantId, balances.get(participantId).add(expenseAmountInGroupCurrency));
                }

                // Subtract their share from their balance (what they owe)
                balances.put(participantId, balances.get(participantId).subtract(splitAmount));
            }
        }

        // Apply completed settlements to adjust balances
        List<Settlement> settlements = settlementRepository.findByGroupIdAndStatus(group.getId(), Settlement.SettlementStatus.COMPLETED);

        for (Settlement settlement : settlements) {
            Long fromId = settlement.getFromParticipant().getId();
            Long toId = settlement.getToParticipant().getId();
            BigDecimal amount = settlement.getAmount();

            // Convert settlement amount to group currency if needed
            BigDecimal settlementAmountInGroupCurrency = amount;
            if (!settlement.getCurrency().equals(group.getDefaultCurrency())) {
                // Settlement amounts should already be in group currency, but handle edge cases
                log.warn("Settlement {} has different currency ({}) than group default currency ({}). Using original amount.",
                        settlement.getId(), settlement.getCurrency().getCode(), group.getDefaultCurrency().getCode());
                // In future versions, implement conversion here if needed
            }

            // Người trả đã trả tiền → cộng lại vào số dư (they get credit)
            balances.put(fromId, balances.get(fromId).add(settlementAmountInGroupCurrency));

            // Người nhận đã nhận tiền → trừ khỏi số dư (their debt is reduced)
            balances.put(toId, balances.get(toId).subtract(settlementAmountInGroupCurrency));
        }

        return balances;
    }

    /**
     * Calculate multi-currency breakdown for transparency
     * This shows what users actually spent in their original currencies
     */
    public Map<String, Map<Long, BigDecimal>> calculateOriginalCurrencyBalances(Group group) {
        Map<String, Map<Long, BigDecimal>> currencyBalances = new HashMap<>();

        // Initialize participants for all currencies
        List<Expense> expenses = expenseRepository.findAllByGroupId(group.getId());
        Set<String> currencies = expenses.stream()
                .map(expense -> expense.getOriginalCurrency().getCode())
                .collect(Collectors.toSet());

        for (String currencyCode : currencies) {
            Map<Long, BigDecimal> participantBalances = new HashMap<>();
            for (GroupParticipant participant : group.getParticipants()) {
                participantBalances.put(participant.getId(), BigDecimal.ZERO);
            }
            currencyBalances.put(currencyCode, participantBalances);
        }

        // Calculate balances by original currency (what users actually paid/owe)
        for (Expense expense : expenses) {
            String originalCurrency = expense.getOriginalCurrency().getCode();
            Map<Long, BigDecimal> participantBalances = currencyBalances.get(originalCurrency);

            for (ExpenseSplit split : expense.getSplits()) {
                if (split.getParticipant() == null) continue;

                Long participantId = split.getParticipant().getId();

                // Calculate split amount in original currency
                BigDecimal splitRatio = split.getAmount().divide(expense.getConvertedAmount(), 6, RoundingMode.HALF_UP);
                BigDecimal originalCurrencySplit = expense.getOriginalAmount().multiply(splitRatio).setScale(2, RoundingMode.HALF_UP);

                // If they paid, add the original amount to their balance
                if (split.isPayer()) {
                    participantBalances.put(participantId,
                            participantBalances.get(participantId).add(expense.getOriginalAmount()));
                }

                // Subtract their share (what they owe in original currency)
                participantBalances.put(participantId,
                        participantBalances.get(participantId).subtract(originalCurrencySplit));
            }
        }

        return currencyBalances;
    }

    public List<BalanceResponse> convertToBalanceResponse(Group group, Map<Long, BigDecimal> balances) {
        return group.getParticipants().stream().map(participant -> {
            String participantUserId = null;
            if (participant.getUser() != null) {
                participantUserId = participant.getUser().getId();
            }

            return BalanceResponse.builder()
                    .groupId(group.getId())
                    .groupName(group.getName())
                    .participantUserId(participantUserId)
                    .participantId(participant.getId())
                    .participantName(participant.getName())
                    .balance(balances.getOrDefault(participant.getId(), BigDecimal.ZERO))
                    .currencyCode(group.getDefaultCurrency().getCode())
                    .build();
        }).collect(Collectors.toList());
    }

    public List<BalanceResponse> getUserBalancesByGroup(String userId) {
        List<Group> groups = groupRepository.findByParticipants_User_Id(userId);
        List<BalanceResponse> result = new ArrayList<>();

        for (Group group : groups) {
            // Tìm participantId của user trong group này
            Optional<GroupParticipant> myParticipantOpt = group.getParticipants().stream()
                    .filter(p -> p.getUser() != null && p.getUser().getId().equals(userId))
                    .findFirst();
            if (myParticipantOpt.isEmpty()) continue;
            GroupParticipant myParticipant = myParticipantOpt.get();

            // Tính balances cho group
            Map<Long, BigDecimal> balances = calculateBalances(group);

            // Lấy số dư của user trong group này
            BigDecimal balance = balances.getOrDefault(myParticipant.getId(), BigDecimal.ZERO);

            result.add(BalanceResponse.builder()
                    .groupId(group.getId())
                    .groupName(group.getName())
                    .participantUserId(userId)
                    .participantId(myParticipant.getId())
                    .participantName(myParticipant.getName())
                    .balance(balance)
                    .currencyCode(group.getDefaultCurrency().getCode())
                    .build());
        }
        return result;
    }

    @Transactional
    public SettlementResponse updateSettlementStatus(Long settlementId, SettlementUpdateRequest request) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new AppException(ErrorCode.SETTLEMENT_NOT_FOUND));

        settlement.setStatus(request.getStatus());
        if (request.getStatus() == Settlement.SettlementStatus.COMPLETED) {
            settlement.setSettledAt(LocalDateTime.now());
        }

        return settlementMapper.toSettlementResponse(settlementRepository.save(settlement));
    }

    public void updateSettlementStatusVnPay(Long settlementId, Settlement.SettlementStatus status, String vnpayTransactionId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new AppException(ErrorCode.SETTLEMENT_NOT_FOUND));
        settlement.setStatus(status);
        settlement.setVnpayTransactionId(vnpayTransactionId);
        if (status == Settlement.SettlementStatus.COMPLETED) {
            settlement.setSettledAt(LocalDateTime.now());
        }
        settlementRepository.save(settlement);
    }

    public List<SettlementResponse> getGroupSettlements(Long groupId) {
        return settlementRepository.findByGroupId(groupId).stream()
                .map(settlementMapper::toSettlementResponse)
                .collect(Collectors.toList());
    }

    public List<SettlementResponse> getUserSettlements(String userId) {
        List<Settlement> settlements = settlementRepository.findByFromParticipant_User_IdOrToParticipant_User_Id(userId, userId);
        return settlements.stream()
                .map(settlementMapper::toSettlementResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SettlementResponse createSettlement(SettlementCreationRequest request) {
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));

        GroupParticipant from = participantRepository.findById(request.getFromParticipantId())
                .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

        GroupParticipant to = participantRepository.findById(request.getToParticipantId())
                .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

        Currency currency = currencyRepository.findByCode(request.getCurrencyCode())
                .orElseThrow(() -> new AppException(ErrorCode.CURRENCY_NOT_EXISTED));

        Settlement settlement = Settlement.builder()
                .group(group)
                .fromParticipant(from)
                .toParticipant(to)
                .amount(request.getAmount())
                .currency(currency)
                .settlementMethod(request.getSettlementMethod())
                .description(request.getDescription())
                .status(request.getStatus())
                .createdAt(LocalDateTime.now())
                .build();

        settlement = settlementRepository.save(settlement);

        User fromUser = from.getUser();
        User toUser = to.getUser();
        String groupName = group.getName();
        String fromName = from.getName();
        String toName = to.getName();
        String amountStr = settlement.getAmount().toPlainString();
        String currencyCode = settlement.getCurrency().getCode();
        // Nếu là yêu cầu thanh toán (người nhận gửi cho người nợ)
        if (request.getSettlementMethod() == null && request.getStatus() == Settlement.SettlementStatus.PENDING) {
            String content = String.format(
                    "Yêu cầu thanh toán từ %s đến %s trong nhóm %s, số tiền %s %s",
                    toName, fromName, groupName, amountStr, currencyCode
            );
            RequestCreationRequest req = RequestCreationRequest.builder()
                    .type("PAYMENT_REQUEST")
                    .receiverId(fromUser.getId())
                    .groupId(group.getId())
                    .referenceId(settlement.getId())
                    .content(content)
                    .build();
            requestService.createRequest(req, toUser);
        }

        // Nếu là xác nhận đã thanh toán (người nợ gửi cho người nhận)
        if (request.getSettlementMethod() != null && request.getStatus() == Settlement.SettlementStatus.PENDING) {
            String content = String.format(
                    "%s xác nhận đã thanh toán cho %s trong nhóm %s, số tiền %s %s",
                    fromName, toName, groupName, amountStr, currencyCode
            );
            RequestCreationRequest req = RequestCreationRequest.builder()
                    .type("PAYMENT_CONFIRM")
                    .receiverId(toUser.getId())
                    .groupId(group.getId())
                    .referenceId(settlement.getId())
                    .content(content)
                    .build();
            requestService.createRequest(req, fromUser);
        }

        return settlementMapper.toSettlementResponse(settlement);
    }
}
