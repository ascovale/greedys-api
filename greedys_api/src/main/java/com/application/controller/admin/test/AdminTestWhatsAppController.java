package com.application.controller.admin.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.WhatsAppService;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * Controller for handling WhatsApp message sending requests.
 */

@RestController
@SecurityRequirement(name = "bearerAuth")
public class AdminTestWhatsAppController {
    @Autowired
    private WhatsAppService whatsappService;

    @Operation(summary = "Send WhatsApp message", description = "Sends a WhatsApp message to the specified phone number")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Message sent successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/send-message")
    public void sendMessage(@RequestBody MessageRequest request) {
        whatsappService.sendWhatsAppMessage(request.getPhoneNumber(), request.getMessage());
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
