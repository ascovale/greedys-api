package com.application.customer.controller.restaurant;

import java.util.Collection;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.dto.ApiResponse;
import com.application.common.web.dto.get.TableDTO;
import com.application.restaurant.service.TableService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Restaurant Tables", description = "Controller for handling requests related to restaurant tables")
@RequestMapping("/customer/restaurant")
@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerTableController extends BaseController {

	private final TableService tableService;

	@GetMapping("/room/{roomId}/tables")
	@ReadApiResponses
	@Operation(summary = "Get tables of a room", description = "Retrieve the tables of a room")
	public ResponseEntity<ApiResponse<Collection<TableDTO>>> getTables(@PathVariable Long roomId) {
		return execute("get tables for room", () -> tableService.findByRoom(roomId));
	}
}
