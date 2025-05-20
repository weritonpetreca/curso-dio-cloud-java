package com.petreca.dto;

import com.petreca.persistence.entity.BoardColumnKindEnum;

public record BoardColumnInfoDTO(Long id, int order, BoardColumnKindEnum kind, String name) {
}
