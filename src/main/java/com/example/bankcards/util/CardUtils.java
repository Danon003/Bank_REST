package com.example.bankcards.util;

public class CardUtils {
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != 16) {
            return cardNumber;
        }
        return "**** **** **** " + cardNumber.substring(12);
    }
}