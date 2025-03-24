 package com.application.security.user;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.application.persistence.model.customer.Customer;
import com.application.web.dto.get.CustomerDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


@Component("userAuthenticationSuccessHandler")
public class UserLoginAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private final ObjectMapper objectMapper = new ObjectMapper();



    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) throws IOException {
      //  System.out.println(getUserName(authentication));


    	response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");

        // Qui puoi ottenere i dettagli dell'utente e convertirli in JSON
        // Ad esempio, supponendo che tu abbia un metodo getUserDetails che restituisce i dati dell'utente
        CustomerDTO userDetails = new CustomerDTO((Customer) authentication.getPrincipal());
        response.getWriter().write(objectMapper.writeValueAsString(userDetails));

        response.getWriter().flush();
    }
/* 
    private String getUserName(final Authentication authentication) {
        return ((Customer) authentication.getPrincipal()).getName();
    }

    private void addWelcomeCookie(final String user, final HttpServletResponse response) {
        Cookie welcomeCookie = getWelcomeCookie(user);
       // System.out.println(user);
        response.addCookie(welcomeCookie);
    }

    private Cookie getWelcomeCookie(final String user) {
        Cookie welcomeCookie = new Cookie("welcome", user);
        welcomeCookie.setMaxAge(60 * 60 * 24 * 30); // 30 days
        return welcomeCookie;
    }
*/
    protected void clearAuthenticationAttributes(final HttpServletRequest request) {
        final HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }

    public void setRedirectStrategy(final RedirectStrategy redirectStrategy) {
        this.redirectStrategy = redirectStrategy;
    }

    protected RedirectStrategy getRedirectStrategy() {
        return redirectStrategy;
    }
}