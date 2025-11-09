package com.application.agency.service.authentication;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import com.application.agency.service.AgencyUserService;
import com.application.common.security.jwt.constants.TokenValidationConstants;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AgencyAuthenticationService {

    private final AgencyUserService agencyUserService;

    /**
     * Conferma la registrazione di un AgencyUserHub tramite token di verifica
     */
    public String confirmAgencyUserHubRegistration(final HttpServletRequest request, final Model model,
            final String token) {
        Locale locale = request.getLocale();
        final String result = agencyUserService.validateHubVerificationToken(token);

        if (TokenValidationConstants.TOKEN_VALID.equals(result)) {
            // Hub verified successfully - all associated AgencyUsers are now enabled
            if (model != null) {
                model.addAttribute("message", "Agency hub account verified successfully!");
            }
            log.info("Agency hub verification successful for token: {}", token);
            return "redirect:/console.html?lang=" + locale.getLanguage();
        }

        String errorMessage = getErrorMessage(result);
        if (model != null) {
            model.addAttribute("message", errorMessage);
            model.addAttribute("expired", TokenValidationConstants.TOKEN_EXPIRED.equals(result));
            model.addAttribute("token", token);
        }
        log.warn("Agency hub verification failed for token: {} - Result: {}", token, result);
        return "redirect:/public/badUser.html?lang=" + locale.getLanguage();
    }

    private String getErrorMessage(String result) {
        switch (result) {
            case TokenValidationConstants.TOKEN_EXPIRED:
                return "Verification token has expired. Please request a new one.";
            case TokenValidationConstants.TOKEN_INVALID:
                return "Invalid verification token.";
            default:
                return "Verification failed. Please try again.";
        }
    }
}