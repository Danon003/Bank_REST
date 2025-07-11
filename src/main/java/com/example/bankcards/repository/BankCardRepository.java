package com.example.bankcards.repository;

import com.example.bankcards.entity.BankCard;
import com.example.bankcards.entity.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankCardRepository extends JpaRepository<BankCard, Integer> {
    Page<BankCard> findByOwner(Person owner, Pageable pageable);
    Optional<BankCard> findByCardNumber(String cardNumber);
    List<BankCard> findByOwnerAndStatus(Person owner, BankCard.CardStatus status);
    boolean existsByCardNumber(String cardNumber);
    Optional<BankCard> findByIdAndOwnerId(Integer cardId, Integer ownerId);
    Page<BankCard> findByStatus(BankCard.CardStatus status, Pageable pageable);
    List<BankCard> findByStatusAndExpiryDateBefore(BankCard.CardStatus status, LocalDate date);
}