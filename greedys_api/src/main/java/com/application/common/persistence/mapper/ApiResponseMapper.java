package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.common.web.ApiResponse;

/**
 * MapStruct mapper per ApiResponse
 * Questo mapper fornisce utilitá per la conversione e trasformazione di ApiResponse
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface ApiResponseMapper {

    ApiResponseMapper INSTANCE = Mappers.getMapper(ApiResponseMapper.class);

    /**
     * Clona un ApiResponse mantenendo la struttura generica
     */
    @Mapping(target = "timestamp", ignore = true) // Sarà aggiornato automaticamente
    <T> ApiResponse<T> clone(ApiResponse<T> original);

    /**
     * Converte un ApiResponse con un tipo di dato in un altro
     * Utile per trasformare il contenuto mantenendo metadata e status
     */
    @Mapping(target = "data", ignore = true) // Il nuovo dato deve essere impostato esternamente
    @Mapping(target = "timestamp", ignore = true) // Sarà aggiornato automaticamente
    <T, R> ApiResponse<R> convertDataType(ApiResponse<T> source);
}
