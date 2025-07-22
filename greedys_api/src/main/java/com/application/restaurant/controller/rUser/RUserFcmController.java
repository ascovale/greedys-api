package com.application.restaurant.controller.rUser;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.dto.post.FcmTokenDTO;
import com.application.restaurant.model.user.RUserFcmToken;
import com.application.restaurant.service.RUserFcmTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/restaurant/user/fcm")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "FCM", description = "Restaurant User FCM token management API")
public class RUserFcmController {

    private final RUserFcmTokenService tokenService;

    public RUserFcmController(RUserFcmTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/{deviceId}")
    @Operation(summary = "Register FCM token", description = "Registers a new FCM token for a device of the restaurant user")
    public ResponseEntity<Void> registerFcmToken(@PathVariable String deviceId, @RequestBody FcmTokenDTO tokenDTO) {
        tokenService.saveUserFcmToken(tokenDTO);
        return ResponseEntity.ok().build();
        //TODO: Improve error handling and response
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "Get FCM token by device ID", description = "Retrieves the FCM token associated with a specific device ID")
    public ResponseEntity<RUserFcmToken> getFcmTokenByDeviceId(@PathVariable String deviceId) {
        RUserFcmToken token = tokenService.getTokenByDeviceId(deviceId);
        if (token == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(token);
    }
        
    
    

}