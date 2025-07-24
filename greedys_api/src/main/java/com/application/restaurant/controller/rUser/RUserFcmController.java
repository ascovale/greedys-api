package com.application.restaurant.controller.rUser;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.web.dto.ApiResponse;
import com.application.common.web.dto.post.FcmTokenDTO;
import com.application.restaurant.persistence.model.user.RUserFcmToken;
import com.application.restaurant.service.RUserFcmTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/restaurant/user/fcm")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "FCM", description = "Restaurant User FCM token management API")
@RequiredArgsConstructor
@Slf4j
public class RUserFcmController extends BaseController {

    private final RUserFcmTokenService tokenService;

    @PostMapping("/{deviceId}")
    @Operation(summary = "Register FCM token", description = "Registers a new FCM token for a device of the restaurant user")
    public ResponseEntity<ApiResponse<String>> registerFcmToken(@PathVariable String deviceId, @RequestBody FcmTokenDTO tokenDTO) {
        return executeCreate("register FCM token", "FCM token registered successfully", () -> {
            tokenService.saveUserFcmToken(tokenDTO);
            return "success";
        });
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "Get FCM token by device ID", description = "Retrieves the FCM token associated with a specific device ID")
    public ResponseEntity<ApiResponse<RUserFcmToken>> getFcmTokenByDeviceId(@PathVariable String deviceId) {
        return execute("get FCM token by device ID", () -> {
            RUserFcmToken token = tokenService.getTokenByDeviceId(deviceId);
            if (token == null) {
                throw new java.util.NoSuchElementException("FCM token not found for device: " + deviceId);
            }
            return token;
        });
    }
}