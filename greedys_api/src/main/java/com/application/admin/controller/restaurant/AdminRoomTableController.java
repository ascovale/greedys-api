package com.application.admin.controller.restaurant;

import java.util.Collection;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.application.common.web.dto.restaurant.RoomDTO;
import com.application.common.web.dto.restaurant.TableDTO;
import com.application.restaurant.service.RoomService;
import com.application.restaurant.service.TableService;
import com.application.restaurant.web.dto.restaurant.NewRoomDTO;
import com.application.restaurant.web.dto.restaurant.NewTableDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/admin/restaurant")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Room & Table", description = "Admin Room and Table Management")
@RequiredArgsConstructor
@Slf4j
public class AdminRoomTableController extends BaseController {

	private final RoomService roomService;
	private final TableService tableService;

	/* -- === *** ROOMS AND TABLES *** === --- */

	@GetMapping(value = "/{restaurantId}/rooms")
	@Operation(summary = "Get rooms of a restaurant", description = "Retrieve the rooms of a restaurant")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_READ')")
	
	@WrapperType(dataClass = RoomDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ListResponseWrapper<RoomDTO>> getRooms(@PathVariable Long restaurantId) {
		return executeList("get rooms", () -> {
			Collection<RoomDTO> rooms = roomService.findByRestaurant(restaurantId);
			return rooms instanceof List ? (List<RoomDTO>) rooms : new java.util.ArrayList<>(rooms);
		});
	}

	@GetMapping(value = "/room/{roomId}/tables")
	@Operation(summary = "Get tables of a room", description = "Retrieve the tables of a room")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_READ')")
	
	@WrapperType(dataClass = TableDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ListResponseWrapper<TableDTO>> getTables(@PathVariable Long roomId) {
		return executeList("get tables", () -> {
			Collection<TableDTO> tables = tableService.findByRoom(roomId);
			return tables instanceof List ? (List<TableDTO>) tables : new java.util.ArrayList<>(tables);
		});
	}

	@PostMapping(value = "/{restaurantId}/room")
	@Operation(summary = "Add a room to a restaurant", description = "Add a new room to a restaurant")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@WrapperType(dataClass = RoomDTO.class, responseCode = "201")
    public ResponseEntity<ResponseWrapper<RoomDTO>> addRoom(@PathVariable Long restaurantId, @RequestBody NewRoomDTO roomDto) {
		return executeCreate("add room", "Room created successfully", () -> {
			return roomService.createRoom(roomDto, restaurantId);
		});
	}

	@PostMapping(value = "/{restaurantId}/table")
	@Operation(summary = "Add a table to a room", description = "Add a new table to a room")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@WrapperType(dataClass = TableDTO.class, type = WrapperDataType.DTO, responseCode = "201")
    public ResponseEntity<ResponseWrapper<TableDTO>> addTable(@PathVariable Long restaurantId, @RequestBody NewTableDTO tableDto) {
		return executeCreate("add table", "Table created successfully", () -> {
			return tableService.createTable(tableDto);
		});
	}
}

