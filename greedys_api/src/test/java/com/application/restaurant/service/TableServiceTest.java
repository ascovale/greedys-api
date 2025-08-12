package com.application.restaurant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import org.mockito.junit.jupiter.MockitoExtension;

import com.application.common.persistence.mapper.TableMapper;
import com.application.common.web.dto.restaurant.TableDTO;
import com.application.restaurant.persistence.dao.RoomDAO;
import com.application.restaurant.persistence.dao.TableDAO;
import com.application.restaurant.persistence.model.Room;
import com.application.restaurant.persistence.model.Table;
import com.application.restaurant.web.dto.restaurant.NewTableDTO;

@ExtendWith(MockitoExtension.class)
class TableServiceTest {

    @Mock
    private TableDAO tableDAO;

    @Mock
    private RoomDAO roomDAO;

    @Mock
    private TableMapper tableMapper;

    @InjectMocks
    private TableService tableService;

    private Table mockTable;
    private Room mockRoom;
    private NewTableDTO newTableDTO;
    private TableDTO mockTableDTO;

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

        mockTableDTO = TableDTO.builder()
                .id(1L)
                .name("Tavolo 1")
                .capacity(4)
                .positionX(100)
                .positionY(200)
                .build();

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
        when(tableMapper.toDTO(mockTable)).thenReturn(mockTableDTO);

        // ACT
        Collection<TableDTO> result = tableService.findByRoom(roomId);

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.size());
        
        TableDTO tableDTO = result.iterator().next();
        assertEquals(mockTableDTO.getId(), tableDTO.getId());
        assertEquals(mockTableDTO.getName(), tableDTO.getName());
        assertEquals(mockTableDTO.getCapacity(), tableDTO.getCapacity());
        
        // Verifica che il DAO sia stato chiamato correttamente
        verify(tableDAO).findByRoom_Id(roomId);
        verify(tableMapper).toDTO(mockTable);
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
        when(roomDAO.findById(newTableDTO.getRoomId())).thenReturn(Optional.of(mockRoom));
        when(tableDAO.save(any(Table.class))).thenReturn(mockTable);

        // ACT
        TableDTO result = tableService.createTable(newTableDTO);

        // ASSERT
        assertNotNull(result);
        assertEquals(mockTable.getId(), result.getId());
        verify(roomDAO).findById(newTableDTO.getRoomId());
        verify(tableDAO).save(any(Table.class));
    }

    @Test
    void createTable_ShouldThrowException_WhenRoomNotFound() {
        // ARRANGE
        when(roomDAO.findById(newTableDTO.getRoomId())).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            tableService.createTable(newTableDTO);
        });

        verify(roomDAO).findById(newTableDTO.getRoomId());
        // Verifica che save NON sia stato chiamato
        verify(tableDAO, never()).save(any(Table.class));
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
    void findById_ShouldThrowException_WhenTableNotFound() {
        // ARRANGE
        Long tableId = 999L;
        when(tableDAO.findById(tableId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            tableService.findById(tableId);
        });

        verify(tableDAO).findById(tableId);
    }

    @Test
    void findByIdAsDTO_ShouldReturnTableDTO_WhenTableExists() {
        // ARRANGE
        Long tableId = 1L;
        when(tableDAO.findById(tableId)).thenReturn(Optional.of(mockTable));
        when(tableMapper.toDTO(mockTable)).thenReturn(mockTableDTO);

        // ACT
        TableDTO result = tableService.findByIdAsDTO(tableId);

        // ASSERT
        assertNotNull(result);
        assertEquals(mockTableDTO.getId(), result.getId());
        assertEquals(mockTableDTO.getName(), result.getName());
        verify(tableDAO).findById(tableId);
        verify(tableMapper).toDTO(mockTable);
    }

    @Test
    void findByIdAsDTO_ShouldThrowException_WhenTableNotFound() {
        // ARRANGE
        Long tableId = 999L;
        when(tableDAO.findById(tableId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            tableService.findByIdAsDTO(tableId);
        });

        verify(tableDAO).findById(tableId);
        verify(tableMapper, never()).toDTO(any());
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
        verify(tableDAO, never()).delete(any(Table.class));
    }

    @Test
    void deleteById_ShouldCallDAODeleteById() {
        // ARRANGE
        Long tableId = 1L;

        // ACT
        tableService.deleteById(tableId);

        // ASSERT
        verify(tableDAO).deleteById(tableId);
    }

    @Test
    void deleteAll_ShouldCallDAODeleteAll() {
        // ACT
        tableService.deleteAll();

        // ASSERT
        verify(tableDAO).deleteAll();
    }

    /**
     * Test per verificare la mappatura dei dati nel createTable
     */
    @Test
    void createTable_ShouldMapDataCorrectly() {
        // ARRANGE
        when(roomDAO.findById(newTableDTO.getRoomId())).thenReturn(Optional.of(mockRoom));
        
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
