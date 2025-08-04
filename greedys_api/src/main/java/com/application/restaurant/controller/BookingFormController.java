package com.application.restaurant.controller;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.ResponseWrapper;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class BookingFormController extends BaseController {

    @Operation(summary = "Get the restaurant booking script")
    @GetMapping("/restaurant-form")
    @ResponseBody
    @ReadApiResponses
    public ResponseEntity<ResponseWrapper<String>> getRestaurantIFrameForm(@RequestParam Long idRestaurant) {
        return execute("get restaurant booking form", () -> {
            ClassPathResource resource = new ClassPathResource("static/restaurant-booking.html");
            byte[] bytes = Files.readAllBytes(Paths.get(resource.getURI()));
            return new String(bytes);
        });
    }
}
