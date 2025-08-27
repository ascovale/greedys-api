package com.application.common.spring.swagger;

/**
 * Enum per i tipi di flusso rilevati dall'analisi statica
 */
public enum ReturnTypeFlow {
    CREATE_201,           // executeCreate() → 201 Created
    READ_200_SINGLE,      // execute() → 200 OK with single object
    READ_200_LIST,        // executeList() → 200 OK with List<T>
    READ_200_PAGINATED,   // executePaginated() → 200 OK with Page<T>
    READ_200_VOID,        // executeVoid() → 200 OK with String
    UNKNOWN               // Pattern non riconosciuto
}
