package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @AfterAll
    public static void tearDown() {

    }

    @BeforeEach
    public void setUpPerTest() {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    }

    @Test
    public void testParkingACar() {
        when(inputReaderUtil.readSelection()).thenReturn(1);

        parkingService.processIncomingVehicle();

        Ticket DBticket = ticketDAO.getTicket("ABCDEF");

        // Verify ticket is saved in DB
        assertNotNull(DBticket);

        // Verify parking availability is updated in DB
        int ticketSpotId = DBticket.getParkingSpot().getId();
        int availableSpotId = parkingSpotDAO.getNextAvailableSlot(DBticket.getParkingSpot().getParkingType());
        assertNotEquals(ticketSpotId, availableSpotId);
    }

    @Test
    public void testParkingLotExit() {
        FareCalculatorService fareCalculatorService = new FareCalculatorService();

        // Create ticket with 2h duration in DB
        Ticket ticket = new Ticket();
        ParkingSpot parkingSpot = new ParkingSpot(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR), ParkingType.CAR, false);
        parkingSpotDAO.updateParking(parkingSpot);
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setPrice(0);
        Date inTime = new Date(System.currentTimeMillis() - (2 * 3600 * 1000));
        ticket.setInTime(inTime);
        ticket.setOutTime(null);
        ticketDAO.saveTicket(ticket);

        parkingService.processExitingVehicle();

        Ticket DBticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(DBticket);

        // Verify out time is set in DB
        Date outTime = DBticket.getOutTime();
        assertNotNull(outTime);

        // Verify generated fare is correctly calculated and set in DB
        double DBprice = DBticket.getPrice();
        boolean isRegularClient = ticketDAO.getNbTicket("ABCDEF") >= parkingService.getRegularClientThreshold();
        fareCalculatorService.calculateFare(DBticket, isRegularClient);
        double expectedPrice = DBticket.getPrice();
        assertEquals(expectedPrice, DBprice);
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        // Add enough tickets to become a regular client
        for (int i = 0; i < parkingService.getRegularClientThreshold() - 1; i++) {
            testParkingACar();
            testParkingLotExit();
        }
    }
}
