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

        double inHour = (double) ticket.getInTime().getTime() / 3600000;
        double outHour = (double) ticket.getOutTime().getTime() / 3600000;
        double duration = outHour - inHour;

        if (duration < 0.5) {
            ticket.setPrice(0);
        } else {
            switch (ticket.getParkingSpot().getParkingType()) {
                case CAR: {
                    double fare = hasDiscount ? Fare.CAR_RATE_PER_HOUR * DISCOUNT : Fare.CAR_RATE_PER_HOUR;
                    ticket.setPrice(duration * fare);
                    break;
                }
                case BIKE: {
                    double fare = hasDiscount ? Fare.BIKE_RATE_PER_HOUR * DISCOUNT : Fare.BIKE_RATE_PER_HOUR;
                    ticket.setPrice(duration * fare);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown Parking Type");
            }
        }
    }
}