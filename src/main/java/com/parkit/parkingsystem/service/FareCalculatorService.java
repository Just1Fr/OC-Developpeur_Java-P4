package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    private static final double DISCOUNT = 0.95;

    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }

    public void calculateFare(Ticket ticket, boolean hasDiscount) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }

        long inHour = ticket.getInTime().getTime();
        long outHour = ticket.getOutTime().getTime();
        double duration = (double) (outHour - inHour) / 3600000; // duration in hours

        double price = 0;
        if (duration >= 0.5) {
            double fare;
            switch (ticket.getParkingSpot().getParkingType()) {
                case CAR: {
                    fare = Fare.CAR_RATE_PER_HOUR;
                    break;
                }
                case BIKE: {
                    fare = Fare.BIKE_RATE_PER_HOUR;
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown Parking Type");
            }
            price = hasDiscount ? duration * fare * DISCOUNT : duration * fare;
        }
        ticket.setPrice(price);
    }
}