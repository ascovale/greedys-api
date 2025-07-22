package com.application.restaurant.controller.restaurant;

import java.util.Collection;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.dto.get.TableDTO;
import com.application.common.web.util.GenericResponse;
import com.application.restaurant.service.TableService;
import com.application.restaurant.web.post.NewTableDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
public class RestaurantTableController {
	
	private final TableService tableService;

	@GetMapping(value = "/room/{roomId}")
	@Operation(summary = "Get tables of a room", description = "Retrieve the tables of a specific room")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TableDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant or room not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<TableDTO>> getTables(@PathVariable Long roomId) {
		log.info("Getting tables for room ID: {}", roomId);
		Collection<TableDTO> tables = tableService.findByRoom(roomId);
		return new ResponseEntity<>(tables, HttpStatus.OK);
	}

	@PostMapping
	@Operation(summary = "Add a table to a room", description = "Add a new table to a specific room")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Restaurant or room not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public GenericResponse addTable(@RequestBody NewTableDTO tableDto) {
		log.info("Adding new table to room");
		tableService.createTable(tableDto);
		return new GenericResponse("success");
	}

	@DeleteMapping(value = "/remove/{tableId}")
	@Operation(summary = "Remove a table", description = "Remove a specific table by its ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "404", description = "Table not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public GenericResponse removeTable(@PathVariable Long tableId) {
		log.info("Removing table with ID: {}", tableId);
		tableService.deleteTable(tableId);
		return new GenericResponse("Table removed successfully");
	}
}
