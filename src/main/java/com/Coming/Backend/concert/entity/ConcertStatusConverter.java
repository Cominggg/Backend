package com.Coming.Backend.concert.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ConcertStatusConverter implements AttributeConverter<ConcertStatus, String> {

    @Override
    public String convertToDatabaseColumn(ConcertStatus status) {
        return status.name();
    }

    @Override
    public ConcertStatus convertToEntityAttribute(String dbValue) {
        return ConcertStatus.valueOf(dbValue);
    }
}
