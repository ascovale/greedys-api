package com.application.admin.controller.test;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.service.WhatsAppService;
import com.application.common.web.ResponseWrapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for handling WhatsApp message sending requests.
 */

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class AdminTestWhatsAppController extends BaseController {
    private final WhatsAppService whatsappService;

    @Operation(summary = "Send WhatsApp message", description = "Sends a WhatsApp message to the specified phone number")
    @CreateApiResponses
    @PostMapping("/send-message")
    public ResponseEntity<ResponseWrapper<String>> sendMessage(@RequestBody MessageRequest request) {
        return executeVoid("send whatsapp message", "Message sent successfully", () -> {
            whatsappService.sendWhatsAppMessage(request.getPhoneNumber(), request.getMessage());
        });
    }

    /**
     * Request body for sending a WhatsApp message.
     */
    public static class MessageRequest {
        private String phoneNumber;
        private String message;

        /**
         * Gets the message to be sent.
         *
         * @return the message
         */
        public String getMessage() {
            return message;
        }

        /**
         * Sets the message to be sent.
         *
         * @param message the message
         */
        public void setMessage(String message) {
            this.message = message;
        }

        /**
         * Gets the phone number to which the message will be sent.
         *
         * @return the phone number
         */
        public String getPhoneNumber() {
            return phoneNumber;
        }

        /**
         * Sets the phone number to which the message will be sent.
         *
         * @param phoneNumber the phone number
         */
        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }
}
