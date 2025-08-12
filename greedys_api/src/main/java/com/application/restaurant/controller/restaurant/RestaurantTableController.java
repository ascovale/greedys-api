package com.application.restaurant.controller.restaurant;

import java.util.Collection;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.WrapperDataType;
import com.application.common.controller.annotation.WrapperType;
import com.application.common.web.ListResponseWrapper;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.restaurant.TableDTO;
import com.application.restaurant.service.TableService;
import com.application.restaurant.web.dto.restaurant.NewTableDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Restaurant Table Management", description = "Controller for managing restaurant table operations")
@RestController
@RequestMapping("/restaurant/table")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class RestaurantTableController extends BaseController {
	
	private final TableService tableService;

	@GetMapping(value = "/room/{roomId}")
	@Operation(summary = "Get tables of a room", description = "Retrieve the tables of a specific room")
	
	@WrapperType(dataClass = TableDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ListResponseWrapper<TableDTO>> getTables(@PathVariable Long roomId) {
		return executeList("get tables for room", () -> {
			log.info("Getting tables for room ID: {}", roomId);
			Collection<TableDTO> tables = tableService.findByRoom(roomId);
			return List.copyOf(tables);
		});
	}

	@PostMapping
	@Operation(summary = "Add a table to a room", description = "Add a new table to a specific room")

	@WrapperType(dataClass = TableDTO.class, type = WrapperDataType.DTO, responseCode = "201")
    public ResponseEntity<ResponseWrapper<TableDTO>> addTable(@RequestBody NewTableDTO tableDto) {
		return executeCreate("add table", "Table added successfully", () -> {
			log.info("Adding new table to room");
			return tableService.createTable(tableDto);
		});
	}

	@DeleteMapping(value = "/remove/{tableId}")
	@Operation(summary = "Remove a table", description = "Remove a specific table by its ID")
	public ResponseEntity<ResponseWrapper<String>> removeTable(@PathVariable Long tableId) {
		return executeVoid("remove table", "Table removed successfully", () -> {
			log.info("Removing table with ID: {}", tableId);
			tableService.deleteTable(tableId);
		});
	}
}
