package com.example.bankcards.controller;

import com.example.bankcards.dto.BankCardDTO;
import com.example.bankcards.entity.BankCard;
import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.security.PersonDetails;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.CardConverter;
import com.example.bankcards.service.PeopleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@RestController
@RequestMapping("/api/cards")
public class CardController {
    private final CardService cardService;
    private final CardConverter cardConverter;
    private final PeopleService peopleService;

    @Autowired
    public CardController(CardService cardService, CardConverter cardConverter, PeopleService peopleService) {
        this.cardService = cardService;
        this.cardConverter = cardConverter;
        this.peopleService = peopleService;
    }


    @GetMapping("/my")
    public Page<BankCardDTO> getUserCards(
            @AuthenticationPrincipal PersonDetails personDetails,
            Pageable pageable) {
        return cardService.getUserCards(personDetails.getPerson(), pageable)
                .map(cardConverter::convertToDto);
    }

    @PostMapping("/{cardId}/request-block")
    public ResponseEntity<Void> requestCardBlock(
            @PathVariable Integer cardId,
            @AuthenticationPrincipal PersonDetails personDetails) {
        cardService.requestCardBlock(personDetails.getPerson().getId(), cardId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transferBetweenCards(
            @RequestParam Integer fromCardId,
            @RequestParam Integer toCardId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description,
            @AuthenticationPrincipal PersonDetails personDetails) {

        Transaction transaction = cardService.transferBetweenCards(
                personDetails.getPerson().getId(),
                fromCardId,
                toCardId,
                amount,
                description);

        return ResponseEntity.ok(transaction);
    }

    // Для администраторов

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all")
    public Page<BankCardDTO> getAllCards(Pageable pageable) {
        return cardService.getAllCards(pageable)
                .map(cardConverter::convertToDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/pending-block")
    public Page<BankCardDTO> getPendingBlockRequests(Pageable pageable) {
        return cardService.getPendingBlockRequests(pageable)
                .map(cardConverter::convertToDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/{cardId}/block")
    public ResponseEntity<BankCardDTO> blockCard(
            @PathVariable Integer cardId) {
        BankCard card = cardService.changeCardStatus(cardId, BankCard.CardStatus.BLOCKED);
        return ResponseEntity.ok(cardConverter.convertToDto(card));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/{cardId}/activate")
    public ResponseEntity<BankCardDTO> activateCard(
            @PathVariable Integer cardId) {
        BankCard card = cardService.changeCardStatus(cardId, BankCard.CardStatus.ACTIVE);
        return ResponseEntity.ok(cardConverter.convertToDto(card));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/{cardId}/confirm-block")
    public ResponseEntity<BankCardDTO> confirmBlockRequest(
            @PathVariable Integer cardId) {
        BankCard card = cardService.confirmBlockCard(cardId);
        return ResponseEntity.ok(cardConverter.convertToDto(card));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/{cardId}/reject-block")
    public ResponseEntity<BankCardDTO> rejectBlockRequest(
            @PathVariable Integer cardId) {
        BankCard card = cardService.rejectBlockRequest(cardId);
        return ResponseEntity.ok(cardConverter.convertToDto(card));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/create")
    public ResponseEntity<?> createCard(@RequestBody BankCardDTO cardDto) {

        try {
            System.out.println(("Creating card for ownerId: {} " + cardDto.getOwnerId()));
            Optional<Person> owner = peopleService.findById(cardDto.getOwnerId());
            System.out.println(("Found owner: {} " + owner));

            BankCard card = convertToEntity(cardDto);
            System.out.println(("Card to save: {} "+ card));

            BankCard createdCard = cardService.createCard(card);
            System.out.println(("Card created: {} "+ createdCard));

            return ResponseEntity.ok(cardConverter.convertToDto(createdCard));
        } catch (Exception e) {
            System.out.println(("Error creating card "+ e));
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    private BankCard convertToEntity(BankCardDTO dto) {
        BankCard card = new BankCard();
        card.setCardNumber(dto.getMaskedCardNumber());
        card.setCardHolder(dto.getCardHolder());

        card.setExpiryDate(dto.getExpiryDate());

        card.setCvv("000");
        card.setBalance(dto.getBalance());
        card.setStatus(BankCard.CardStatus.valueOf(dto.getStatus()));


        Person owner = peopleService.findById(dto.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Owner not found"));
        card.setOwner(owner);

        return card;
    }
}