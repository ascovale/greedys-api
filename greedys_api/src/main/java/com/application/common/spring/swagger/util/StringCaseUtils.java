package com.application.common.spring.swagger.util;

/**
 * Utility per conversione tra convenzioni di naming.
 */
public final class StringCaseUtils {

    private StringCaseUtils() {}

    /**
     * Converte una stringa CamelCase (o simile) in snake_case.
     * Gestisce anche sequenze di maiuscole consecutive (es. "AA" -> "a_a").
     */
    public static String toSnakeCase(String input) {
        if (input == null || input.trim().isEmpty()) return input;
    // Prima separiamo gli acronimi seguiti da una parola in Camel (es. "HTMLParser" -> "HTML_Parser").
    // Questo preserva sequenze di maiuscole come "DTO" senza spezzarle in "D_T_O".
    String result = input.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2");

    // Poi split tra minuscola/cifra e maiuscola (es. "userId" -> "user_Id").
    result = result.replaceAll("([a-z0-9])([A-Z])", "$1_$2");

    return result.toLowerCase();
    }
}
