package com.application.controller.rUser;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.controller.utils.ControllerUtils;
import com.application.persistence.model.fcm.RUserFcmToken;
import com.application.persistence.model.notification.RestaurantNotification;
import com.application.service.RUserFcmTokenService;
import com.application.service.RestaurantNotificationService;
import com.application.web.dto.post.FcmTokenDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


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