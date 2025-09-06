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

        // 1) Split tra una maiuscola seguita da una maiuscola+minuscole (es. "HTMLParser" -> "HTML_Parser")
        String result = input.replaceAll("([A-Z])([A-Z][a-z])", "$1_$2");
        // 2) Split tra minuscola/cifra e maiuscola (es. "userId" -> "user_Id")
        result = result.replaceAll("([a-z0-9])([A-Z])", "$1_$2");
        // 3) Split tra maiuscole consecutive rimanenti (es. "AA" -> "A_A")
        result = result.replaceAll("([A-Z])([A-Z])", "$1_$2");

        return result.toLowerCase();
    }
}
