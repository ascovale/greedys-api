package com.application.controller.restaurantUser;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;

@RestController

public class BookingFormController {


    // TODO  Puoi fare questo aggiungendo l'intestazione X-Frame-Options con il valore ALLOW-FROM o SAMEORIGIN nella risposta HTTP.
    @Operation(summary = "Get the restaurant booking script")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Script retrieved successfully",
                     content = { @Content(mediaType = "text/html") }),
        @ApiResponse(responseCode = "404", description = "Script not found",
                     content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                     content = @Content)
    })
    @GetMapping("/restaurant-form")
    @ResponseBody
    public String getRestaurantIFrameForm(@RequestParam Long idRestaurant) throws IOException {
        //TODO Codice completamente da scrivere concettualmente prende un id e restituisce html da inserire nella pagina web del ristorante
        ClassPathResource resource = new ClassPathResource("static/restaurant-booking.html");
        byte[] bytes = Files.readAllBytes(Paths.get(resource.getURI()));
        return new String(bytes);
    }
}
