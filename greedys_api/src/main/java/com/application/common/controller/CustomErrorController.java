package com.application.common.controller;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
    
        // Log dettagliati
        System.out.println("Error Status Code: " + status);
        System.out.println("Error Message: " + errorMessage);
        if (exception != null) {
            System.out.println("Exception: " + exception);
            Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
            if (throwable != null) {
                System.out.println("Exception Message: " + throwable.getMessage());
                throwable.printStackTrace();
            }
        }
    
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
    
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("error", "Pagina non trovata");
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                model.addAttribute("error", "Errore interno del server");
            } else {
                model.addAttribute("error", "Errore sconosciuto. Codice: " + statusCode);
            }
        } else {
            model.addAttribute("error", "Errore sconosciuto.");
        }
    
        return "error";
    }
    
}