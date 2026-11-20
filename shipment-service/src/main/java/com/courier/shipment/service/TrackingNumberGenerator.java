package com.courier.shipment.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class TrackingNumberGenerator {
    private static final char[] ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public TrackingNumberGenerator() { this(Clock.systemUTC()); }
    TrackingNumberGenerator(Clock clock) { this.clock = clock; }

    public String generate() {
        StringBuilder suffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) suffix.append(ALPHANUMERIC[random.nextInt(ALPHANUMERIC.length)]);
        String date = LocalDate.now(clock.withZone(ZoneOffset.UTC)).format(DAY);
        return "CT-" + date + "-" + suffix;
    }
}
