package com.application.restaurant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.application.common.web.dto.get.TableDTO;
import com.application.restaurant.persistence.dao.TableDAO;
import com.application.restaurant.persistence.model.Room;
import com.application.restaurant.persistence.model.Table;
import com.application.restaurant.web.dto.post.NewTableDTO;

import jakarta.persistence.EntityManager;

/**
 * Test per TableService
 * 
 * Questo test dimostra come testare i servizi con mock:
 * - Mock dei DAO (repository)
 * - Mock di EntityManager
 * - Test della logica business
 * - Verifica delle chiamate ai DAO
 * - Test di casi edge e errori
 */
@ExtendWith(MockitoExtension.class)
class TableServiceTest {

    @Mock
    private TableDAO tableDAO;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TableService tableService;

    private Table mockTable;
    private Room mockRoom;
    private NewTableDTO newTableDTO;

    @BeforeEach
    void setUp() {
        // Prepare test data
        mockRoom = new Room();
        mockRoom.setId(1L);
        mockRoom.setName("Sala Principale");

        mockTable = new Table();
        mockTable.setId(1L);
        mockTable.setName("Tavolo 1");
        mockTable.setRoom(mockRoom);
        mockTable.setCapacity(4);
        mockTable.setPositionX(100);
        mockTable.setPositionY(200);

        newTableDTO = new NewTableDTO();
        newTableDTO.setRoomId(1L);
        newTableDTO.setName("Nuovo Tavolo");
        newTableDTO.setCapacity(6);
        newTableDTO.setPositionX(150);
        newTableDTO.setPositionY(250);
    }

    @Test
    void findByRoom_ShouldReturnTableDTOs_WhenTablesExist() {
        // ARRANGE
        Long roomId = 1L;
        when(tableDAO.findByRoom_Id(roomId)).thenReturn(Arrays.asList(mockTable));

        // ACT
        Collection<TableDTO> result = tableService.findByRoom(roomId);

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.size());
        
        TableDTO tableDTO = result.iterator().next();
        assertEquals(mockTable.getId(), tableDTO.getId());
        assertEquals(mockTable.getName(), tableDTO.getName());
        assertEquals(mockTable.getCapacity(), tableDTO.getCapacity());
        
        // Verifica che il DAO sia stato chiamato correttamente
        verify(tableDAO).findByRoom_Id(roomId);
    }

    @Test
    void findByRoom_ShouldReturnEmptyCollection_WhenNoTablesExist() {
        // ARRANGE
        Long roomId = 1L;
        when(tableDAO.findByRoom_Id(roomId)).thenReturn(Arrays.asList());

        // ACT
        Collection<TableDTO> result = tableService.findByRoom(roomId);

        // ASSERT
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(tableDAO).findByRoom_Id(roomId);
    }

    @Test
    void createTable_ShouldSaveTable_WhenValidData() {
        // ARRANGE
        when(entityManager.getReference(Room.class, newTableDTO.getRoomId()))
                .thenReturn(mockRoom);
        when(tableDAO.save(any(Table.class))).thenReturn(mockTable);

        // ACT
        tableService.createTable(newTableDTO);

        // ASSERT
        verify(entityManager).getReference(Room.class, newTableDTO.getRoomId());
        verify(tableDAO).save(any(Table.class));
    }

    @Test
    void createTable_ShouldThrowException_WhenRoomNotFound() {
        // ARRANGE
        when(entityManager.getReference(Room.class, newTableDTO.getRoomId()))
                .thenThrow(new RuntimeException("Room not found"));

        // ACT & ASSERT
        assertThrows(RuntimeException.class, () -> {
            tableService.createTable(newTableDTO);
        });

        verify(entityManager).getReference(Room.class, newTableDTO.getRoomId());
        // Verifica che save NON sia stato chiamato
        verify(tableDAO, Mockito.never()).save(any(Table.class));
    }

    @Test
    void findById_ShouldReturnTable_WhenTableExists() {
        // ARRANGE
        Long tableId = 1L;
        when(tableDAO.findById(tableId)).thenReturn(Optional.of(mockTable));

        // ACT
        Table result = tableService.findById(tableId);

        // ASSERT
        assertNotNull(result);
        assertEquals(mockTable.getId(), result.getId());
        assertEquals(mockTable.getName(), result.getName());
        verify(tableDAO).findById(tableId);
    }

    @Test
    void findById_ShouldReturnNull_WhenTableNotFound() {
        // ARRANGE
        Long tableId = 999L;
        when(tableDAO.findById(tableId)).thenReturn(Optional.empty());

        // ACT
        Table result = tableService.findById(tableId);

        // ASSERT
        assertNull(result);
        verify(tableDAO).findById(tableId);
    }

    @Test
    void deleteTable_ShouldDeleteTable_WhenTableExists() {
        // ARRANGE
        Long tableId = 1L;
        when(tableDAO.findById(tableId)).thenReturn(Optional.of(mockTable));

        // ACT
        tableService.deleteTable(tableId);

        // ASSERT
        verify(tableDAO).findById(tableId);
        verify(tableDAO).delete(mockTable);
    }

    @Test
    void deleteTable_ShouldThrowException_WhenTableNotFound() {
        // ARRANGE
        Long tableId = 999L;
        when(tableDAO.findById(tableId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            tableService.deleteTable(tableId);
        });

        verify(tableDAO).findById(tableId);
        // Verifica che delete NON sia stato chiamato
        verify(tableDAO, Mockito.never()).delete(any(Table.class));
    }

    /**
     * Test per verificare la mappatura dei dati
     */
    @Test
    void createTable_ShouldMapDataCorrectly() {
        // ARRANGE
        when(entityManager.getReference(Room.class, newTableDTO.getRoomId()))
                .thenReturn(mockRoom);
        
        // Capture dell'argomento passato a save
        ArgumentCaptor<Table> tableCaptor = ArgumentCaptor.forClass(Table.class);
        when(tableDAO.save(tableCaptor.capture())).thenReturn(mockTable);

        // ACT
        tableService.createTable(newTableDTO);

        // ASSERT
        Table savedTable = tableCaptor.getValue();
        assertEquals(newTableDTO.getName(), savedTable.getName());
        assertEquals(newTableDTO.getCapacity(), savedTable.getCapacity());
        assertEquals(newTableDTO.getPositionX(), savedTable.getPositionX());
        assertEquals(newTableDTO.getPositionY(), savedTable.getPositionY());
        assertEquals(mockRoom, savedTable.getRoom());
    }
}
