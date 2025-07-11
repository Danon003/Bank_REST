package com.example.bankcards.service;

import com.example.bankcards.dto.BankCardDTO;
import com.example.bankcards.entity.BankCard;
import com.example.bankcards.util.CardUtils;
import org.springframework.stereotype.Service;

@Service
public class CardConverter {

    public BankCardDTO convertToDto(BankCard card) {
        BankCardDTO dto = new BankCardDTO();
        dto.setMaskedCardNumber(CardUtils.maskCardNumber(card.getCardNumber()));
        dto.setCardHolder(card.getCardHolder());
        dto.setExpiryDate(card.getExpiryDate());
        dto.setBalance(card.getBalance());
        dto.setStatus(card.getStatus().name());
        dto.setOwnerId(card.getOwner().getId());
        dto.setOwnerUsername(card.getOwner().getUsername());
        return dto;
    }
}