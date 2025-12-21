package com.exchange.util;

import com.exchange.model.Balance;
import com.exchange.model.Currency;
import com.exchange.model.Participant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataReader {
    private static final Logger logger = LogManager.getLogger(DataReader.class);

    public static List<Participant> readParticipants(String filePath) throws IOException {
        List<Participant> participants = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                try {
                    Participant participant = parseParticipant(line);
                    participants.add(participant);
                    logger.info("Loaded participant: {}", participant.getName());
                } catch (Exception e) {
                    logger.error("Error parsing line {}: {}", lineNumber, e.getMessage());
                }
            }
        }

        logger.info("Total {} participants loaded from {}", participants.size(), filePath);
        return participants;
    }

    private static Participant parseParticipant(String line) {
        String[] parts = line.split(";");

        if (parts.length < 5) {
            throw new IllegalArgumentException("Invalid format: expected at least 5 fields");
        }

        String name = parts[0].trim();
        BigDecimal mntBalance = new BigDecimal(parts[1].trim());
        BigDecimal usdBalance = new BigDecimal(parts[2].trim());
        BigDecimal eurBalance = new BigDecimal(parts[3].trim());
        int maxConcurrentTransactions = Integer.parseInt(parts[4].trim());

        Map<Currency, BigDecimal> initialBalance = new HashMap<>();
        initialBalance.put(Currency.MNT, mntBalance);
        initialBalance.put(Currency.USD, usdBalance);
        initialBalance.put(Currency.EUR, eurBalance);

        Balance balance = new Balance(initialBalance);

        return new Participant(name, balance, maxConcurrentTransactions);
    }

    public static class ConfigData {
        private final List<Participant> participants;
        private final int numberOfTransactionsPerParticipant;
        private final long reportIntervalMs;

        public ConfigData(List<Participant> participants,
                         int numberOfTransactionsPerParticipant,
                         long reportIntervalMs) {
            this.participants = participants;
            this.numberOfTransactionsPerParticipant = numberOfTransactionsPerParticipant;
            this.reportIntervalMs = reportIntervalMs;
        }

        public List<Participant> getParticipants() {
            return participants;
        }

        public int getNumberOfTransactionsPerParticipant() {
            return numberOfTransactionsPerParticipant;
        }

        public long getReportIntervalMs() {
            return reportIntervalMs;
        }
    }

    public static ConfigData readConfig(String participantsFile) throws IOException {
        List<Participant> participants = readParticipants(participantsFile);

        int numberOfTransactionsPerParticipant = 10;
        long reportIntervalMs = 3000;

        return new ConfigData(participants, numberOfTransactionsPerParticipant, reportIntervalMs);
    }
}
