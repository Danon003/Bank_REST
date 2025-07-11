package com.example.bankcards.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_card")
public class BankCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "card_number", nullable = false, unique = true)
    @Size(min = 16, max = 16, message = "Номер карты должен содержать 16 цифр")
    @Pattern(regexp = "\\d+", message = "Номер карты должен содержать только цифры")
    private String cardNumber;

    @Column(name = "card_holder", nullable = false)
    @NotBlank(message = "Имя владельца не может быть пустым")
    private String cardHolder;

    @Column(name = "expiry_date", nullable = false)
    @Future(message = "Срок действия карты должен быть в будущем")
    private LocalDateTime expiryDate;

    @Column(name = "cvv", nullable = false)
    @Size(min = 3, max = 4, message = "CVV должен содержать 3 или 4 цифры")
    private String cvv;

    @Column(name = "balance",columnDefinition = "numeric", nullable = false)
    @DecimalMin(value = "0.0", message = "Баланс не может быть отрицательным")
    private double balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CardStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Person owner;

    public enum CardStatus {
        ACTIVE, BLOCKED, EXPIRED, PENDING_BLOCK
    }

    public BankCard() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardHolder() {
        return cardHolder;
    }

    public void setCardHolder(String cardHolder) {
        this.cardHolder = cardHolder;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public Person getOwner() {
        return owner;
    }

    public void setOwner(Person owner) {
        this.owner = owner;
    }
}