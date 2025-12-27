package com.example.dxvision.domain.admin.dto;

public record LesionDataDto(
        String type,
        Double cx,
        Double cy,
        Double r,
        Double x,
        Double y,
        Double w,
        Double h
) {
}
