package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static final Logger logger = LogManager.getLogger("ParkingServiceTest");

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    private static ParkingService parkingService;
    private ParkingSpot parkingSpot;
    private Ticket ticket;

    @BeforeEach
    public void setUpPerTest() {
        try {
            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
            parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
            ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // 1h duration
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");

            // Return mocked ticket
            lenient().when(ticketDAO.getTicket(ticket.getVehicleRegNumber())).thenReturn(ticket);
            // Return mocked RegNumber
            lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(ticket.getVehicleRegNumber());
            // Regular client
            lenient().when(ticketDAO.getNbTicket(ticket.getVehicleRegNumber())).thenReturn(3);
            // Select CAR
            lenient().when(inputReaderUtil.readSelection()).thenReturn(1);
            // Return mocked parkingSpot id
            lenient().when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(parkingSpot.getId());
        } catch (Exception e) {
            logger.error("Failed to set up test mock objects", e);
            throw new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void processExitingVehicle() {
        // Successfully update ticket
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

        parkingService.processExitingVehicle();

        verify(inputReaderUtil, times(1)).readVehicleRegistrationNumber();
        verify(ticketDAO, times(1)).getTicket(ticket.getVehicleRegNumber());
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
    }

    @Test
    public void processExitingVehicleUnableUpdate() {
        // Fail to update ticket
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        parkingService.processExitingVehicle();

        verify(inputReaderUtil, times(1)).readVehicleRegistrationNumber();
        verify(ticketDAO, times(1)).getTicket(ticket.getVehicleRegNumber());
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
    }

    @Test
    public void processIncomingVehicle() {
        parkingService.processIncomingVehicle();

        verify(inputReaderUtil, times(1)).readVehicleRegistrationNumber();
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(any(ParkingType.class));
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
    }

    @Test
    public void getNextParkingNumberIfAvailable() {
        assertEquals(parkingSpot.getId(), parkingService.getNextParkingNumberIfAvailable().getId());
    }

    @Test
    public void getNextParkingNumberIfAvailableParkingNumberNotFound() {
        // No parking spot available
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(-1);

        assertNull(parkingService.getNextParkingNumberIfAvailable());
    }

    @Test
    public void getNextParkingNumberIfAvailableParkingNumberWrongArgument() {
        // Invalid vehicle type selection
        when(inputReaderUtil.readSelection()).thenReturn(-1);

        assertNull(parkingService.getNextParkingNumberIfAvailable());
    }
}