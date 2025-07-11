package com.example.bankcards.service;

import com.example.bankcards.entity.BankCard;
import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.repository.BankCardRepository;
import com.example.bankcards.repository.PeopleRepository;
import com.example.bankcards.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CardService {
    private final BankCardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final PeopleRepository peopleRepository;

    @Autowired
    public CardService(BankCardRepository cardRepository,
                       TransactionRepository transactionRepository,
                       PeopleRepository peopleRepository) {
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.peopleRepository = peopleRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public BankCard createCard(BankCard card) {
        if (cardRepository.existsByCardNumber(card.getCardNumber())) {
            throw new CardOperationException("Карта с таким номером уже существует");
        }
        if (card.getCardNumber() == null || card.getCardNumber().length() != 16) {
            throw new IllegalArgumentException("Invalid card number");
        }
        return cardRepository.save(card);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public BankCard changeCardStatus(Integer cardId, BankCard.CardStatus newStatus) {
        BankCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardOperationException("Карта не найдена"));

        if (newStatus == BankCard.CardStatus.PENDING_BLOCK) {
            throw new CardOperationException("Нельзя установить статус PENDING_BLOCK напрямую");
        }

        card.setStatus(newStatus);
        return cardRepository.save(card);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public BankCard confirmBlockCard(Integer cardId) {
        BankCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardOperationException("Карта не найдена"));

        if (card.getStatus() != BankCard.CardStatus.PENDING_BLOCK) {
            throw new CardOperationException("Нет запроса на блокировку для этой карты");
        }

        card.setStatus(BankCard.CardStatus.BLOCKED);
        return cardRepository.save(card);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public BankCard rejectBlockRequest(Integer cardId) {
        BankCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardOperationException("Карта не найдена"));

        if (card.getStatus() != BankCard.CardStatus.PENDING_BLOCK) {
            throw new CardOperationException("Нет запроса на блокировку для этой карты");
        }

        card.setStatus(BankCard.CardStatus.ACTIVE);
        return cardRepository.save(card);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<BankCard> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<BankCard> getPendingBlockRequests(Pageable pageable) {
        return cardRepository.findByStatus(BankCard.CardStatus.PENDING_BLOCK, pageable);
    }

    // USER METHODS

    @PreAuthorize("#user.id == authentication.principal.id")
    public Page<BankCard> getUserCards(Person user, Pageable pageable) {
        return cardRepository.findByOwner(user, pageable);
    }

    @PreAuthorize("#userId == authentication.principal.id")
    public void requestCardBlock(Integer userId, Integer cardId) {
        BankCard card = cardRepository.findByIdAndOwnerId(cardId, userId)
                .orElseThrow(() -> new CardOperationException("Карта не найдена или не принадлежит пользователю"));

        if (card.getStatus() == BankCard.CardStatus.BLOCKED) {
            throw new CardOperationException("Карта уже заблокирована");
        }

        if (card.getStatus() == BankCard.CardStatus.PENDING_BLOCK) {
            throw new CardOperationException("Запрос на блокировку уже отправлен");
        }

        card.setStatus(BankCard.CardStatus.PENDING_BLOCK);
        cardRepository.save(card);
    }

    @Transactional
    @PreAuthorize("#userId == authentication.principal.id")
    public Transaction transferBetweenCards(Integer userId,
                                            Integer fromCardId,
                                            Integer toCardId,
                                            BigDecimal amount,
                                            String description) {
        BankCard fromCard = cardRepository.findByIdAndOwnerId(fromCardId, userId)
                .orElseThrow(() -> new CardOperationException("Исходная карта не найдена"));

        BankCard toCard = cardRepository.findByIdAndOwnerId(toCardId, userId)
                .orElseThrow(() -> new CardOperationException("Целевая карта не найдена"));

        if (fromCard.getStatus() != BankCard.CardStatus.ACTIVE ||
                toCard.getStatus() != BankCard.CardStatus.ACTIVE) {
            throw new CardOperationException("Одна из карт не активна");
        }

        if (fromCard.getBalance() < amount.doubleValue()) {
            throw new CardOperationException("Недостаточно средств");
        }

        fromCard.setBalance(fromCard.getBalance() - amount.doubleValue());
        toCard.setBalance(toCard.getBalance() + amount.doubleValue());

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        Transaction transaction = new Transaction();
        transaction.setFromCard(fromCard);
        transaction.setToCard(toCard);
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setDescription(description);

        return transactionRepository.save(transaction);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void checkAndUpdateExpiredCards() {
        List<BankCard> cards = cardRepository.findByStatusAndExpiryDateBefore(
                BankCard.CardStatus.ACTIVE, LocalDate.now());

        cards.forEach(card -> card.setStatus(BankCard.CardStatus.EXPIRED));
        cardRepository.saveAll(cards);
    }

}