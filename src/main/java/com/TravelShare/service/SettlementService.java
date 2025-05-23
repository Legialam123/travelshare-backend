package com.TravelShare.service;

import com.TravelShare.dto.request.SettlementCreationRequest;
import com.TravelShare.dto.request.SettlementUpdateRequest;
import com.TravelShare.dto.response.BalanceResponse;
import com.TravelShare.dto.response.SettlementResponse;
import com.TravelShare.entity.*;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public List<SettlementResponse> suggestSettlements(Long tripId) {
        return suggestSettlements(tripId, null); // Gọi lại hàm chính với username = null
    }

    public List<SettlementResponse> suggestSettlements(Long groupId, String username) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));

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

        debtors.sort((a, b) -> b.getValue().abs().compareTo(a.getValue().abs()));
        creditors.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<SettlementResponse> suggestions = new ArrayList<>();

        int i = 0, j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            Long debtorId = debtors.get(i).getKey();
            Long creditorId = creditors.get(j).getKey();

            BigDecimal debtAmount = debtors.get(i).getValue().abs();
            BigDecimal creditAmount = creditors.get(j).getValue();
            BigDecimal amount = debtAmount.min(creditAmount);

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
                    .amount(amount)
                    .currencyCode(group.getDefaultCurrency().getCode())
                    .status(Settlement.SettlementStatus.SUGGESTED)
                    .description("Gợi ý thanh toán từ hệ thống")
                    .build();

            suggestions.add(suggestion);

            debtors.get(i).setValue(debtors.get(i).getValue().add(amount));
            creditors.get(j).setValue(creditors.get(j).getValue().subtract(amount));

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
                BigDecimal amount = split.getAmount();

                // Convert to trip's default currency if needed
                if (!expense.getCurrency().equals(group.getDefaultCurrency())) {
                    // Implement currency conversion logic here if needed
                }

                // If they paid, add to their balance
                if (split.isPayer()) {
                    balances.put(participantId, balances.get(participantId).add(expense.getAmount()));
                }
                    balances.put(participantId, balances.get(participantId).subtract(amount));
            }
        }
        List<Settlement> settlements = settlementRepository.findByGroupIdAndStatus(group.getId(), Settlement.SettlementStatus.COMPLETED);

        for (Settlement s : settlements) {
            Long fromId = s.getFromParticipant().getId();
            Long toId = s.getToParticipant().getId();
            BigDecimal amount = s.getAmount();

            // Người trả đã trả tiền → cộng lại vào số dư
            balances.put(fromId, balances.get(fromId).add(amount));

            // Người nhận đã nhận tiền → trừ khỏi số dư
            balances.put(toId, balances.get(toId).subtract(amount));
        }
        return balances;
    }

    public List<BalanceResponse> convertToBalanceResponse(Group group, Map<Long, BigDecimal> balances) {
        return group.getParticipants().stream().map(participant -> {
            String participantUserId = null;
            if (participant.getUser() != null) {
                participantUserId = participant.getUser().getId();
            }

            return BalanceResponse.builder()
                    .participantUserId(participantUserId)
                    .participantId(participant.getId())
                    .participantName(participant.getName())
                    .balance(balances.getOrDefault(participant.getId(), BigDecimal.ZERO))
                    .currencyCode(group.getDefaultCurrency().getCode())
                    .build();
        }).collect(Collectors.toList());
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

    public List<SettlementResponse> getGroupSettlements(Long groupId) {
        return settlementRepository.findByGroupId(groupId).stream()
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
        return settlementMapper.toSettlementResponse(settlement);
    }
}
