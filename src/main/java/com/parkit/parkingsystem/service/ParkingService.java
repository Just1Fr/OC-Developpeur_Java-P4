package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;

public class ParkingService {

    private static final Logger logger = LogManager.getLogger("ParkingService");

    private static final FareCalculatorService fareCalculatorService = new FareCalculatorService();
    private static final int REGULAR_CLIENT_THRESHOLD = 3;
    private final InputReaderUtil inputReaderUtil;
    private final ParkingSpotDAO parkingSpotDAO;
    private final TicketDAO ticketDAO;

    public ParkingService(InputReaderUtil inputReaderUtil, ParkingSpotDAO parkingSpotDAO, TicketDAO ticketDAO) {
        this.inputReaderUtil = inputReaderUtil;
        this.parkingSpotDAO = parkingSpotDAO;
        this.ticketDAO = ticketDAO;
    }

    public int getRegularClientThreshold() {
        return REGULAR_CLIENT_THRESHOLD;
    }

    private boolean isRegularClient(TicketDAO ticketDAO, String vehicleRegNumber) {
        return ticketDAO.getNbTicket(vehicleRegNumber) >= REGULAR_CLIENT_THRESHOLD;
    }

    public void processIncomingVehicle() {
        try {
            ParkingSpot parkingSpot = getNextParkingNumberIfAvailable();
            if (parkingSpot != null && parkingSpot.getId() > 0) {
                String vehicleRegNumber = getVehicleRegNumber();

                if (isRegularClient(ticketDAO, vehicleRegNumber)) {
                    System.out.println("Welcome back ! As a regular customer, you will receive a 5% discount !");
                }

                parkingSpot.setAvailable(false);
                if (parkingSpotDAO.updateParking(parkingSpot)) {
                    Date inTime = new Date();
                    Ticket ticket = new Ticket();
                    // ID (auto-generated), PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME
                    ticket.setParkingSpot(parkingSpot);
                    ticket.setVehicleRegNumber(vehicleRegNumber);
                    ticket.setPrice(0);
                    ticket.setInTime(inTime);
                    ticket.setOutTime(null);
                    if (ticketDAO.saveTicket(ticket)) {
                        System.out.println("Successfully generated and saved a ticket.");
                        System.out.println("Please park your vehicle in spot number: " + parkingSpot.getId());
                        System.out.println("Recorded in-time for vehicle number " + vehicleRegNumber + ": " + inTime);
                    } else {
                        System.out.println("Error: Unable to save ticket information.");
                        // Set available back to true if saveTicket() fails
                        parkingSpot.setAvailable(true);
                        parkingSpotDAO.updateParking(parkingSpot);
                    }
                } else {
                    System.out.println("Error: Unable to assign parking spot.");
                    // Set available back to true if updateParking() fails, to reflect database state
                    parkingSpot.setAvailable(true);
                }
            }
        } catch (Exception e) {
            logger.error("Unable to process incoming vehicle", e);
        }
    }

    private String getVehicleRegNumber() {
        System.out.println("Please type the vehicle registration number and press enter key: ");
        return inputReaderUtil.readVehicleRegistrationNumber();
    }

    public ParkingSpot getNextParkingNumberIfAvailable() {
        int parkingNumber;
        ParkingSpot parkingSpot = null;
        try {
            ParkingType parkingType = getVehicleType();
            parkingNumber = parkingSpotDAO.getNextAvailableSlot(parkingType);
            if (parkingNumber > 0) {
                parkingSpot = new ParkingSpot(parkingNumber, parkingType, true);
            } else {
                throw new Exception("Error fetching parking number from DB. Parking slots might be full.");
            }
        } catch (IllegalArgumentException ie) {
            logger.error("Error parsing user input for type of vehicle", ie);
        } catch (Exception e) {
            logger.error("Error fetching next available parking slot", e);
        }
        return parkingSpot;
    }

    private ParkingType getVehicleType() {
        System.out.println("Please select vehicle type from menu:");
        System.out.println("1.CAR");
        System.out.println("2.BIKE");
        int input = inputReaderUtil.readSelection();
        switch (input) {
            case 1: {
                return ParkingType.CAR;
            }
            case 2: {
                return ParkingType.BIKE;
            }
            default: {
                System.out.println("Incorrect input provided");
                throw new IllegalArgumentException("Incorrect vehicle type inputted");
            }
        }
    }

    public void processExitingVehicle() {
        try {
            String vehicleRegNumber = getVehicleRegNumber();
            Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);
            Date outTime = new Date();
            ticket.setOutTime(outTime);
            boolean hasDiscount = isRegularClient(ticketDAO, vehicleRegNumber);
            fareCalculatorService.calculateFare(ticket, hasDiscount);
            if (ticketDAO.updateTicket(ticket)) {
                ParkingSpot parkingSpot = ticket.getParkingSpot();
                parkingSpot.setAvailable(true);
                if (!parkingSpotDAO.updateParking(parkingSpot)) {
                    // Set available back to false if updateParking() fails, to reflect database state
                    parkingSpot.setAvailable(false);
                }
                System.out.println("Please pay the parking fare: " + ticket.getPrice());
                System.out.println("Recorded out-time for vehicle number " + ticket.getVehicleRegNumber() + ": " + outTime);
            } else {
                System.out.println("Error: Unable to update ticket information.");
            }
        } catch (Exception e) {
            logger.error("Unable to process exiting vehicle", e);
        }
    }
}
