package com.TravelShare.service;

import com.TravelShare.dto.request.ExpenseCreationRequest;
import com.TravelShare.dto.request.ExpenseSplitCreationRequest;
import com.TravelShare.dto.request.ExpenseSplitUpdateRequest;
import com.TravelShare.dto.request.ExpenseUpdateRequest;
import com.TravelShare.dto.response.ExpenseResponse;
import com.TravelShare.entity.*;
import com.TravelShare.entity.Currency;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.mapper.ExpenseMapper;
import com.TravelShare.repository.*;
import jakarta.transaction.Transactional;
import jdk.jfr.Category;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ExpenseService {
    ExpenseMapper expenseMapper;
    ExpenseRepository expenseRepository;
    UserRepository userRepository;
    TripRepository tripRepository;
    CurrencyRepository currencyRepository;
    ExpenseCategoryRepository expenseCategoryRepository;
    ExpenseSplitRepository expenseSplitRepository;
    TripParticipantRepository tripParticipantRepository;
    MediaRepository mediaRepository;

    public ExpenseResponse getExpense(Long expenseId) {
        return expenseMapper.toExpenseResponse(expenseRepository
                .findById(expenseId)
                .orElseThrow(() -> new AppException(ErrorCode.EXPENSE_NOT_EXISTED)));
    }

    public List<ExpenseResponse> getAllExpenses() {
        log.info("In method get Expense");
        return expenseRepository.findAll()
                .stream()
                .map(expenseMapper::toExpenseResponse).toList();
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseCreationRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        ExpenseCategory category = expenseCategoryRepository.findById(request.getCategory())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));

        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_EXISTED));

        User payer = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Create expense
        Expense expense = expenseMapper.toExpense(request);
        expense.setSplits(new HashSet<>());
        // Validate and retrieve entities
        Currency currency;
        if (request.getCurrency() == null || request.getCurrency().isEmpty()) {
            // Use trip's default currency when not specified
            currency = trip.getDefaultCurrency();
        } else {
            // Use the currency specified in the request
            currency = currencyRepository.findByCode(request.getCurrency())
                    .orElseThrow(() -> new AppException(ErrorCode.CURRENCY_NOT_EXISTED));
        }
        expense.setCurrency(currency);
        expense.setCategory(category);
        expense.setTrip(trip);
        expense.setPayer(payer);
        expense.setCreatedAt(LocalDateTime.now());

        // Handle attachments if any
        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            Set<Media> attachments = new HashSet<>();
            for (Long mediaId : request.getAttachmentIds()) {
                Media media = mediaRepository.findById(mediaId)
                        .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_EXISTED));
                media.setExpense(expense);
                attachments.add(media);
            }
            expense.setAttachments(attachments);
        }

        switch (request.getSplitType()) {
            case EQUAL:
                createEqualSplits(expense, trip);
                break;
            case AMOUNT:
                createAmountSplits(expense, request.getSplits());
                break;
            case PERCENTAGE:
                createPercentageSplits(expense, request.getSplits());
                break;
            default:
                throw new AppException(ErrorCode.INVALID_SPLIT_TYPE);
        }
        expenseRepository.save(expense);
        return expenseMapper.toExpenseResponse(expense);
    }

    private void createAmountSplits(Expense expense, Set<ExpenseSplitCreationRequest> splitRequests) {
        for (ExpenseSplitCreationRequest splitRequest : splitRequests) {
            ExpenseSplit split = ExpenseSplit.builder()
                    .expense(expense)
                    .amount(splitRequest.getAmount())
                    .payer(splitRequest.isPayer())
                    .settlementStatus(ExpenseSplit.SettlementStatus.PENDING)
                    .build();
            if (splitRequest.getParticipantId() != null) {
                TripParticipant participant = tripParticipantRepository
                        .findById(splitRequest.getParticipantId())
                        .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));
                split.setParticipant(participant);
            }
            expense.getSplits().add(split);
        }
    }

    private void createPercentageSplits(Expense expense, Set<ExpenseSplitCreationRequest> splitRequests) {
        for (ExpenseSplitCreationRequest splitRequest : splitRequests) {
            BigDecimal amount = expense.getAmount().multiply(splitRequest.getPercentage()).divide(BigDecimal.valueOf(100));
            ExpenseSplit split = ExpenseSplit.builder()
                    .expense(expense)
                    .amount(amount)
                    .percentage(splitRequest.getPercentage())
                    .payer(splitRequest.isPayer())
                    .settlementStatus(ExpenseSplit.SettlementStatus.PENDING)
                    .build();
            if (splitRequest.getParticipantId() != null) {
                TripParticipant participant = tripParticipantRepository
                        .findById(splitRequest.getParticipantId())
                        .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));
                split.setParticipant(participant);
            }
            expense.getSplits().add(split);
        }
    }

    /*
    private void createSharesSplits(Expense expense, Set<ExpenseSplitCreationRequest> splitRequests) {
        int totalShares = splitRequests.stream().mapToInt(ExpenseSplitCreationRequest::getShares).sum();
        for (ExpenseSplitCreationRequest splitRequest : splitRequests) {
            BigDecimal amount = expense.getAmount().multiply(BigDecimal.valueOf(splitRequest.getShares())).divide(BigDecimal.valueOf(totalShares), RoundingMode.HALF_UP);
            ExpenseSplit split = ExpenseSplit.builder()
                    .expense(expense)
                    .amount(amount)
                    .shares(splitRequest.getShares())
                    .payer(splitRequest.isPayer())
                    .settlementStatus(ExpenseSplit.SettlementStatus.PENDING)
                    .build();
            if (splitRequest.getParticipantId() != null) {
                TripParticipant participant = tripParticipantRepository
                        .findById(splitRequest.getParticipantId())
                        .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));
                split.setParticipant(participant);
            }
            expense.getSplits().add(split);
        }
    }*/

    private void createEqualSplits(Expense expense, Trip trip) {
        BigDecimal count = BigDecimal.valueOf(trip.getParticipants().size());
        BigDecimal equalAmount = expense.getAmount().divide(count, 2, RoundingMode.HALF_UP);
        for (TripParticipant participant : trip.getParticipants()) {
            boolean isPayer = participant.getUser() != null &&
                    participant.getUser().getId().equals(expense.getPayer().getId());
            ExpenseSplit split = ExpenseSplit.builder()
                    .expense(expense)
                    .participant(participant)
                    .amount(equalAmount)
                    .percentage(BigDecimal.valueOf(100).divide(count, 2, RoundingMode.HALF_UP))
                    .payer(isPayer)
                    .settlementStatus(ExpenseSplit.SettlementStatus.PENDING)
                    .build();
            expense.getSplits().add(split);
        }
    }

    @Transactional
    public ExpenseResponse updateExpense(Long expenseId, ExpenseUpdateRequest request) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new AppException(ErrorCode.EXPENSE_NOT_EXISTED));

        expenseMapper.updateExpense(expense, request);

        if (request.getCurrency() != null) {
            Currency currency = currencyRepository.findByCode(request.getCurrency())
                    .orElseThrow(() -> new AppException(ErrorCode.CURRENCY_NOT_EXISTED));
            expense.setCurrency(currency);
        }

        if (request.getCategory() != null) {
            ExpenseCategory category = expenseCategoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));
            expense.setCategory(category);
        }

        if (request.getAttachmentIds() != null) {
            if (!request.getAttachmentIds().isEmpty()) {
                List<Media> attachments = mediaRepository.findAllById(request.getAttachmentIds());
                for (Media media : attachments) {
                    media.setExpense(expense);
                    expense.getAttachments().add(media);
                }
            }
        }
        if (request.getSplitType() != expense.getSplitType() || request.getSplits() != null) {
            expense.setSplitType(request.getSplitType());

            if (request.getSplitType() == Expense.SplitType.EQUAL) {
                // Nếu splits là null hoặc rỗng, tạo từ trip participants
                if (request.getSplits() == null || request.getSplits().isEmpty()) {
                    Set<ExpenseSplitUpdateRequest> autoSplits = expense.getTrip().getParticipants().stream()
                            .map(participant -> {
                                ExpenseSplitUpdateRequest split = new ExpenseSplitUpdateRequest();
                                split.setParticipantId(participant.getId());
                                return split;
                            })
                            .collect(Collectors.toSet());
                    request.setSplits(autoSplits);
                };
                // Tiến hành xử lý bình thường
                updateExpenseSplits(expense, request.getSplits());
            } else {
                // Trường hợp AMOUNT, PERCENTAGE, SHARES
                if (request.getSplits() == null || request.getSplits().isEmpty()) {
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }
                updateExpenseSplits(expense, request.getSplits());
            }
        }
        return expenseMapper.toExpenseResponse(expenseRepository.save(expense));
    }
    private void updateExpenseSplits(Expense expense, Set<ExpenseSplitUpdateRequest> splitRequests){
        // 1. Tạo danh sách splits hiện tại dạng Map<participantId, ExpenseSplit>
        Map<Long, ExpenseSplit> currentSplitsMap = expense.getSplits().stream()
                .filter(s->s.getParticipant() != null)
                .collect(Collectors.toMap(s->s.getParticipant().getId(), s->s));
        // 2. Tập các participantId xuất hiện trong splitRequests
        Set<Long> requestParticipantIds = splitRequests.stream()
                .map(ExpenseSplitUpdateRequest::getParticipantId)
                .collect(Collectors.toSet());
        // 3. Duyệt từng split trong request -> update/create
        for (ExpenseSplitUpdateRequest request : splitRequests) {
            Long participantId = request.getParticipantId();

            TripParticipant participant = tripParticipantRepository.findById(participantId)
                    .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

            if(currentSplitsMap.containsKey(participantId)){
                // Đã tồn tại → update
                ExpenseSplit existingSplit = currentSplitsMap.get(participantId);
                updateSplitFields(existingSplit, request);
            } else {
                // Không có → tạo mới
                ExpenseSplit newSplit = new ExpenseSplit();
                newSplit.setExpense(expense);
                newSplit.setParticipant(participant);
                updateSplitFields(newSplit, request);
                expense.getSplits().add(newSplit);
            }
        }
        // 4. Xóa những splits không còn trong splitRequests
        List<ExpenseSplit> toRemove = expense.getSplits().stream()
                .filter(s -> s.getParticipant() != null)
                .filter(s -> !requestParticipantIds.contains(s.getParticipant().getId()))
                .toList();
        expense.getSplits().removeAll(toRemove);
    }

    private void updateSplitFields(ExpenseSplit split, ExpenseSplitUpdateRequest request) {
        switch (split.getExpense().getSplitType()) {
            case EQUAL -> {
                split.setAmount(split.getExpense().getAmount()
                        .divide(BigDecimal.valueOf(split.getExpense().getSplits().size()), 2, RoundingMode.HALF_UP));
                split.setPercentage(BigDecimal.valueOf(100)
                        .divide(BigDecimal.valueOf(split.getExpense().getSplits().size()), 2, RoundingMode.HALF_UP));
            }
            case AMOUNT ->
                    {
                        split.setAmount(request.getAmount());
                        split.setPercentage(null);
                    }

            case PERCENTAGE -> {
                split.setPercentage(request.getPercentage());
                BigDecimal amount = split.getExpense().getAmount()
                        .multiply(request.getPercentage())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                split.setAmount(amount);
            }
        }

        if (request.getSettlementStatus() != null) {
            split.setSettlementStatus(request.getSettlementStatus());
            if (request.getSettlementStatus() == ExpenseSplit.SettlementStatus.SETTLED) {
                split.setSettledAt(LocalDateTime.now());
            }
        }

        if (request.getProofImageIds() != null && !request.getProofImageIds().isEmpty()) {
            List<Media> mediaList = mediaRepository.findAllById(request.getProofImageIds());
            for (Media media : mediaList) {
                media.setExpenseSplit(split);
                split.getProofImages().add(media);
            }
        }
    }


    public void deleteExpense(Long expenseId) {
        expenseRepository.deleteById(expenseId);
    }
}
