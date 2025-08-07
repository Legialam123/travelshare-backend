package com.TravelShare.service;

import com.TravelShare.dto.request.ExpenseCreationRequest;
import com.TravelShare.dto.request.ExpenseSplitCreationRequest;
import com.TravelShare.dto.request.ExpenseSplitUpdateRequest;
import com.TravelShare.dto.request.ExpenseUpdateRequest;
import com.TravelShare.dto.response.CurrencyConversionResponse;
import com.TravelShare.dto.response.ExpenseResponse;
import com.TravelShare.dto.response.UserExpenseSummaryResponse;
import com.TravelShare.entity.*;
import com.TravelShare.entity.Currency;
import com.TravelShare.event.ExpenseCreatedEvent;
import com.TravelShare.event.ExpenseDeletedEvent;
import com.TravelShare.event.ExpenseUpdatedEvent;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.mapper.ExpenseMapper;
import com.TravelShare.repository.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    GroupRepository groupRepository;
    CurrencyRepository currencyRepository;
    CategoryRepository categoryRepository;
    GroupParticipantRepository groupParticipantRepository;
    MediaRepository mediaRepository;
    ApplicationEventPublisher eventPublisher;
    ExchangeRateService exchangeRateService;

    public ExpenseResponse getExpense(Long expenseId) {
        return expenseMapper.toExpenseResponse(expenseRepository
                .findById(expenseId)
                .orElseThrow(() -> new AppException(ErrorCode.EXPENSE_NOT_EXISTED)));
    }

    public List<ExpenseResponse> getAllExpenses() {
        return expenseRepository.findAll()
                .stream()
                .map(expenseMapper::toExpenseResponse).toList();
    }

    public List<ExpenseResponse> getAllExpensesByGroupId(Long groupId) {
        return expenseRepository.findAllByGroupId(groupId)
                .stream()
                .map(expenseMapper::toExpenseResponse).toList();
    }

    private Specification<Expense> expenseFilterSpec(
            String userId,
            LocalDate startDate,
            LocalDate endDate,
            Long groupId,
            Long categoryId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("payer").get("user").get("id"), userId));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expenseDate"), endDate));
            }
            if (groupId != null) {
                predicates.add(cb.equal(root.get("group").get("id"), groupId));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public UserExpenseSummaryResponse getUserExpenseSummary(
            String userId,
            LocalDate startDate,
            LocalDate endDate,
            Long groupId,
            Long categoryId
    ) {
        Specification<Expense> spec = expenseFilterSpec(userId, startDate, endDate, groupId, categoryId);
        List<Expense> expenses = expenseRepository.findAll(spec);

        // Calculate multi-currency totals by original currency (what user actually spent)
        Map<String, BigDecimal> totalsByOriginalCurrency = expenses.stream()
                .collect(Collectors.groupingBy(
                        expense -> expense.getOriginalCurrency().getCode(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Expense::getOriginalAmount,
                                BigDecimal::add
                        )
                ));

        // Calculate overall total using converted amounts (for meaningful comparison)
        // This represents total spending in a unified currency base
        BigDecimal total = expenses.stream()
                .map(Expense::getConvertedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ExpenseResponse> expenseResponses = expenses.stream()
                .map(expenseMapper::toExpenseResponse)
                .collect(Collectors.toList());

        return new UserExpenseSummaryResponse(total, totalsByOriginalCurrency, expenseResponses);
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseCreationRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Category category = categoryRepository.findById(request.getCategory())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));

        GroupParticipant payer = groupParticipantRepository.findById(request.getParticipantId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Expense expense = expenseMapper.toExpense(request);

        if (expense.getSplits() == null) {
            expense.setSplits(new HashSet<>());
        }

        Currency originalCurrency;
        if (request.getCurrency() == null || request.getCurrency().isEmpty()) {
            // Use trip's default currency when not specified
            originalCurrency = group.getDefaultCurrency();
        } else {
            // Use the currency specified in the request
            originalCurrency = currencyRepository.findByCode(request.getCurrency())
                    .orElseThrow(() -> new AppException(ErrorCode.CURRENCY_NOT_EXISTED));
        }

        // Set currency fields
        expense.setOriginalCurrency(originalCurrency);
        expense.setConvertedCurrency(group.getDefaultCurrency());
        expense.setCategory(category);
        expense.setGroup(group);
        expense.setPayer(payer);
        expense.setCreatedAt(LocalDateTime.now());
        expense.setCreatedBy(user);

        // Set original amount (what user actually spent)
        expense.setOriginalAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));

        // Handle currency conversion
        if (!expense.getOriginalCurrency().equals(expense.getConvertedCurrency())) {
            try {
                CurrencyConversionResponse conversionResponse = exchangeRateService.convertAmount(
                        expense.getOriginalAmount(),
                        expense.getOriginalCurrency().getCode(),
                        expense.getConvertedCurrency().getCode()
                );

                // Check if conversion was successful
                if (conversionResponse.isSuccess()) {
                    expense.setExchangeRate(conversionResponse.getExchangeRate());
                    expense.setExchangeRateDate(LocalDateTime.now());
                    expense.setConvertedAmount(conversionResponse.getConvertedAmount().setScale(2, RoundingMode.HALF_UP));
                } else {
                    // Fallback: use original amount as converted amount
                    log.warn("Currency conversion failed: {}", conversionResponse.getErrorMessage());
                    expense.setConvertedAmount(expense.getOriginalAmount());
                    expense.setExchangeRate(BigDecimal.ONE);
                    expense.setExchangeRateDate(LocalDateTime.now());
                    // Also update converted currency to match original
                    expense.setConvertedCurrency(expense.getOriginalCurrency());
                }
            } catch (Exception e) {
                log.error("Currency conversion error: {}", e.getMessage());
                // Fallback: use original amount as converted amount
                expense.setConvertedAmount(expense.getOriginalAmount());
                expense.setExchangeRate(BigDecimal.ONE);
                expense.setExchangeRateDate(LocalDateTime.now());
                expense.setConvertedCurrency(expense.getOriginalCurrency());
            }
        } else {
            // Same currency, no conversion needed
            expense.setConvertedAmount(expense.getOriginalAmount());
            expense.setExchangeRate(BigDecimal.ONE);
            expense.setExchangeRateDate(LocalDateTime.now());
        }

        switch (request.getSplitType()) {
            case EQUAL:
                log.info("Expense equal: ");
                createEqualSplits(expense, group);
                break;
            case AMOUNT:
                log.info("Expense amount: ");
                createAmountSplits(expense, request.getSplits());
                break;
            case PERCENTAGE:
                log.info("Expense percentage: ");
                createPercentageSplits(expense, request.getSplits());
                break;
            default:
                throw new AppException(ErrorCode.INVALID_SPLIT_TYPE);
        }

        expenseRepository.save(expense);

        eventPublisher.publishEvent(new ExpenseCreatedEvent(this, expense, user));
        return expenseMapper.toExpenseResponse(expense);
    }

    private void createAmountSplits(Expense expense, Set<ExpenseSplitCreationRequest> splitRequests) {
        // Use convertedAmount for splits to ensure fair sharing in group currency
        for (ExpenseSplitCreationRequest splitRequest : splitRequests) {
            BigDecimal percentage = splitRequest.getAmount().divide(expense.getConvertedAmount(), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            boolean isPayer = splitRequest.getParticipantId().equals(expense.getPayer().getId());
            ExpenseSplit split = ExpenseSplit.builder()
                    .expense(expense)
                    .amount(splitRequest.getAmount())
                    .percentage(percentage)
                    .payer(isPayer)
                    .build();

            if (splitRequest.getParticipantId() != null) {
                GroupParticipant participant = groupParticipantRepository
                        .findById(splitRequest.getParticipantId())
                        .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));
                split.setParticipant(participant);
            }
            expense.getSplits().add(split);
        }
    }

    private void createPercentageSplits(Expense expense, Set<ExpenseSplitCreationRequest> splitRequests) {
        // Use convertedAmount for splits to ensure fair sharing in group currency
        for (ExpenseSplitCreationRequest splitRequest : splitRequests) {
            boolean isPayer = splitRequest.getParticipantId().equals(expense.getPayer().getId());
            BigDecimal amount = expense.getConvertedAmount().multiply(splitRequest.getPercentage()).divide(BigDecimal.valueOf(100),2, RoundingMode.HALF_UP);
            ExpenseSplit split = ExpenseSplit.builder()
                    .expense(expense)
                    .amount(amount)
                    .percentage(splitRequest.getPercentage())
                    .payer(isPayer)
                    .build();
            if (splitRequest.getParticipantId() != null) {
                GroupParticipant participant = groupParticipantRepository
                        .findById(splitRequest.getParticipantId())
                        .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));
                split.setParticipant(participant);
            }
            expense.getSplits().add(split);
        }
    }

    private void createEqualSplits(Expense expense, Group group) {
        // Use convertedAmount for splits to ensure fair sharing in group currency
        BigDecimal count = BigDecimal.valueOf(group.getParticipants().size());
        BigDecimal equalAmount = expense.getConvertedAmount().divide(count, 2, RoundingMode.HALF_UP);
        for (GroupParticipant participant : group.getParticipants()) {
            boolean isPayer = participant.getId().equals(expense.getPayer().getId());
            ExpenseSplit split = ExpenseSplit.builder()
                    .expense(expense)
                    .participant(participant)
                    .amount(equalAmount)
                    .percentage(BigDecimal.valueOf(100).divide(count, 2, RoundingMode.HALF_UP))
                    .payer(isPayer)
                    .build();
            expense.getSplits().add(split);
        }
    }

    @Transactional
    public ExpenseResponse updateExpense(Long expenseId, ExpenseUpdateRequest request) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new AppException(ErrorCode.EXPENSE_NOT_EXISTED));
        // Validate if expense is locked
        if (expense.getIsLocked() != null && expense.getIsLocked()) {
            throw new AppException(ErrorCode.EXPENSE_LOCKED);
        }
        expenseMapper.updateExpense(expense, request);

        if(request.getParticipantId() != null){
            GroupParticipant payer = groupParticipantRepository.findById(request.getParticipantId())
                    .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));
            expense.setPayer(payer);
        }

        if (request.getCurrency() != null) {
            Currency originalCurrency = currencyRepository.findByCode(request.getCurrency())
                    .orElseThrow(() -> new AppException(ErrorCode.CURRENCY_NOT_EXISTED));
            expense.setOriginalCurrency(originalCurrency);

            // Recalculate conversion if currency changed
            if (request.getAmount() != null) {
                expense.setOriginalAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
                recalculateCurrencyConversion(expense);
            }
        } else if (request.getAmount() != null) {
            // Amount changed but currency stayed the same
            expense.setOriginalAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
            recalculateCurrencyConversion(expense);
        }

        if (request.getCategory() != null) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));
            expense.setCategory(category);
        }

        if (request.getAttachmentIds() != null) {
            List<Media> currentAttachments = new ArrayList<>(expense.getAttachments());

            // 1. Xóa liên kết những Media không còn nằm trong attachmentIds mới
            for (Media media : currentAttachments) {
                if (!request.getAttachmentIds().contains(media.getId())) {
                    media.setExpense(null);
                    expense.getAttachments().remove(media);
                }
            }

            // 2. Gán thêm những Media mới vào expense (nếu chưa có)
            List<Media> newAttachments = mediaRepository.findAllById(request.getAttachmentIds());
            for (Media media : newAttachments) {
                if (!expense.getAttachments().contains(media)) {
                    media.setExpense(expense);
                    expense.getAttachments().add(media);
                }
            }
        }

        boolean hasSplitType = request.getSplitType() != null;
        boolean hasSplits = request.getSplits() != null;
        boolean hasAmount = request.getAmount() != null;

        boolean needUpdateSplits = hasSplitType || hasSplits || hasAmount;
        if (needUpdateSplits) {
            expense.setSplitType(request.getSplitType());

            if (request.getSplitType() == Expense.SplitType.EQUAL) {
                Set<ExpenseSplitUpdateRequest> autoSplits = expense.getGroup().getParticipants().stream()
                        .map(participant -> {
                            ExpenseSplitUpdateRequest split = new ExpenseSplitUpdateRequest();
                            split.setParticipantId(participant.getId());
                            return split;
                        })
                        .collect(Collectors.toSet());
                updateExpenseSplits(expense, autoSplits);
            } else {
                // Trường hợp AMOUNT, PERCENTAGE
                if (request.getSplits() == null || request.getSplits().isEmpty()) {
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }
                updateExpenseSplits(expense, request.getSplits());
            }
        }
        Expense expenseSaved = expenseRepository.save(expense);
        eventPublisher.publishEvent(new ExpenseUpdatedEvent(this, expenseSaved, expenseSaved.getCreatedBy()));
        return expenseMapper.toExpenseResponse(expenseSaved);
    }

    private void recalculateCurrencyConversion(Expense expense) {
        if (!expense.getOriginalCurrency().equals(expense.getGroup().getDefaultCurrency())) {
            try {
                CurrencyConversionResponse conversionResponse = exchangeRateService.convertAmount(
                        expense.getOriginalAmount(),
                        expense.getOriginalCurrency().getCode(),
                        expense.getGroup().getDefaultCurrency().getCode()
                );

                if (conversionResponse.isSuccess()) {
                    expense.setConvertedCurrency(expense.getGroup().getDefaultCurrency());
                    expense.setExchangeRate(conversionResponse.getExchangeRate());
                    expense.setExchangeRateDate(LocalDateTime.now());
                    expense.setConvertedAmount(conversionResponse.getConvertedAmount().setScale(2, RoundingMode.HALF_UP));
                } else {
                    log.warn("Currency conversion failed during update: {}", conversionResponse.getErrorMessage());
                    expense.setConvertedAmount(expense.getOriginalAmount());
                    expense.setConvertedCurrency(expense.getOriginalCurrency());
                    expense.setExchangeRate(BigDecimal.ONE);
                    expense.setExchangeRateDate(LocalDateTime.now());
                }
            } catch (Exception e) {
                log.error("Currency conversion error during update: {}", e.getMessage());
                expense.setConvertedAmount(expense.getOriginalAmount());
                expense.setConvertedCurrency(expense.getOriginalCurrency());
                expense.setExchangeRate(BigDecimal.ONE);
                expense.setExchangeRateDate(LocalDateTime.now());
            }
        } else {
            // Same currency, no conversion needed
            expense.setConvertedAmount(expense.getOriginalAmount());
            expense.setConvertedCurrency(expense.getOriginalCurrency());
            expense.setExchangeRate(BigDecimal.ONE);
            expense.setExchangeRateDate(LocalDateTime.now());
        }
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

            GroupParticipant participant = groupParticipantRepository.findById(participantId)
                    .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

            boolean isPayer = participantId.equals(expense.getPayer().getId());

            if(currentSplitsMap.containsKey(participantId)){
                // Đã tồn tại → update
                ExpenseSplit existingSplit = currentSplitsMap.get(participantId);
                updateSplitFields(existingSplit, request, expense.getSplitType(), isPayer);
            } else {
                // Không có → tạo mới
                ExpenseSplit newSplit = new ExpenseSplit();
                newSplit.setExpense(expense);
                newSplit.setParticipant(participant);
                updateSplitFields(newSplit, request, expense.getSplitType(), isPayer);
                expense.getSplits().add(newSplit);
            }
        }
        // 4. Xóa những splits không còn trong splitRequests
        List<ExpenseSplit> toRemove = expense.getSplits().stream()
                .filter(s -> s.getParticipant() != null)
                .filter(s -> !requestParticipantIds.contains(s.getParticipant().getId()))
                .toList();
        toRemove.forEach(expense.getSplits()::remove);
    }

    private void updateSplitFields(ExpenseSplit split, ExpenseSplitUpdateRequest request, Expense.SplitType splitType, boolean isPayer) {
        // Use convertedAmount for split calculations to ensure fair sharing in group currency
        switch (splitType) {
            case EQUAL -> {
                int totalParticipants = split.getExpense().getGroup().getParticipants().size();
                split.setAmount(split.getExpense().getConvertedAmount()
                        .divide(BigDecimal.valueOf(totalParticipants), 2, RoundingMode.HALF_UP));
                split.setPercentage(BigDecimal.valueOf(100)
                        .divide(BigDecimal.valueOf(totalParticipants), 2, RoundingMode.HALF_UP));
            }
            case AMOUNT -> {
                split.setAmount(request.getAmount());
                if (split.getExpense().getConvertedAmount().compareTo(BigDecimal.ZERO) > 0) {
                    split.setPercentage(request.getAmount()
                            .divide(split.getExpense().getConvertedAmount(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP));
                }
            }
            case PERCENTAGE -> {
                split.setPercentage(request.getPercentage());
                BigDecimal amount = split.getExpense().getConvertedAmount()
                        .multiply(request.getPercentage())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                split.setAmount(amount);
            }
        }

        split.setPayer(isPayer);
    }

    public void deleteExpense(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new AppException(ErrorCode.EXPENSE_NOT_EXISTED));
        // Validate if expense is locked
        if (expense.getIsLocked() != null && expense.getIsLocked()) {
            throw new AppException(ErrorCode.EXPENSE_LOCKED);
        }
        expenseRepository.deleteById(expenseId);
        eventPublisher.publishEvent(new ExpenseDeletedEvent(this, expense, expense.getCreatedBy()));
    }
}
