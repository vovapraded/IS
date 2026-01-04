package org.example.domain.route.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.util.Base64;

/**
 * Композитный cursor для эффективной пагинации без подзапросов
 *
 * @param sortValue JSON-encoded значение поля сортировки
 */
public record CompositeCursor(String sortField, String sortValue, Integer id, String sortDirection) {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @JsonCreator
    public CompositeCursor(
            @JsonProperty("sortField") String sortField,
            @JsonProperty("sortValue") String sortValue,
            @JsonProperty("id") Integer id,
            @JsonProperty("sortDirection") String sortDirection) {
        this.sortField = sortField;
        this.sortValue = sortValue;
        this.id = id;
        this.sortDirection = sortDirection;
    }

    // Фабричные методы для создания cursor'ов

    public static CompositeCursor forId(Integer id, String sortDirection) {
        return new CompositeCursor("id", id != null ? id.toString() : null, id, sortDirection);
    }

    public static CompositeCursor forName(String name, Integer id, String sortDirection) {
        return new CompositeCursor("name", name, id, sortDirection);
    }

    public static CompositeCursor forDistance(Long distance, Integer id, String sortDirection) {
        return new CompositeCursor("distance", distance != null ? distance.toString() : null, id, sortDirection);
    }

    public static CompositeCursor forRating(Long rating, Integer id, String sortDirection) {
        return new CompositeCursor("rating", rating != null ? rating.toString() : null, id, sortDirection);
    }

    public static CompositeCursor forCreationDate(java.time.LocalDateTime creationDate, Integer id, String sortDirection) {
        return new CompositeCursor("creationDate",
                creationDate != null ? creationDate.toString() : null, id, sortDirection);
    }

    public static CompositeCursor forCreationDate(java.time.ZonedDateTime creationDate, Integer id, String sortDirection) {
        return new CompositeCursor("creationDate",
                creationDate != null ? creationDate.toString() : null, id, sortDirection);
    }

    // Сериализация в Base64-encoded JSON

    public String encode() {
        try {
            String json = objectMapper.writeValueAsString(this);
            return Base64.getUrlEncoder().encodeToString(json.getBytes());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to encode cursor", e);
        }
    }

    public static CompositeCursor decode(String encoded) {
        if (encoded == null || encoded.trim().isEmpty()) {
            return null;
        }

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encoded);
            String json = new String(decoded);
            return objectMapper.readValue(json, CompositeCursor.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor format", e);
        }
    }

    // Методы доступа к типизированным значениям

    public String getStringValue() {
        return sortValue;
    }

    public Long getLongValue() {
        return sortValue != null ? Long.parseLong(sortValue) : null;
    }

    public Integer getIntegerValue() {
        return sortValue != null ? Integer.parseInt(sortValue) : null;
    }

    public java.time.LocalDateTime getLocalDateTimeValue() {
        return sortValue != null ? java.time.LocalDateTime.parse(sortValue) : null;
    }

    public java.time.ZonedDateTime getZonedDateTimeValue() {
        return sortValue != null ? java.time.ZonedDateTime.parse(sortValue) : null;
    }

    // Getters

    @Override
    public String toString() {
        return String.format("CompositeCursor{sortField='%s', sortValue='%s', id=%s, direction='%s'}",
                sortField, sortValue, id, sortDirection);
    }

    // Вспомогательные методы для проверки направления сортировки

    public boolean isDescending() {
        return "desc".equalsIgnoreCase(sortDirection);
    }

    public boolean isAscending() {
        return !isDescending();
    }
}