package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper per la conversione di AuthResponseDTO
 * 
 * NOTE: This mapper is no longer needed as AuthResponseDTO now has proper constructors
 * that accept UserAuthResponse types directly. Kept for backward compatibility reference.
 * 
 * @deprecated Use AuthResponseDTO constructors directly instead
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface AuthResponseMapper {
    // Deprecated - use AuthResponseDTO constructors instead
}
