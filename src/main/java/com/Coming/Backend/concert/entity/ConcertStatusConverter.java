package com.Coming.Backend.concert.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ConcertStatusConverter implements AttributeConverter<ConcertStatus, String> {

    @Override
    public String convertToDatabaseColumn(ConcertStatus status) {
        return status.toDisplayName();
    }

    @Override
    public ConcertStatus convertToEntityAttribute(String dbValue) {
        return switch (dbValue) {
            case "공연예정" -> ConcertStatus.UPCOMING;
            case "공연중" -> ConcertStatus.ONGOING;
            case "공연완료" -> ConcertStatus.ENDED;
            case "공연취소" -> ConcertStatus.CANCELLED;
            default -> throw new IllegalArgumentException("Unknown ConcertStatus value: " + dbValue);
        };
    }
}
