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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for reading application configuration and participant data from files.
 * <p>
 * This class is responsible for loading participants and basic runtime parameters
 * required to start the exchange simulation.
 * </p>
 *
 * <p>
 * The participants file must contain one participant per line
 * in the following semicolon-separated format:
 * </p>
 *
 * <pre>
 * name;MNT_BALANCE;USD_BALANCE;EUR_BALANCE;MAX_CONCURRENT_TRANSACTIONS
 * </pre>
 *
 * <p>
 * Lines that are empty or start with {@code #} are ignored.
 * </p>
 */
public class DataReader {

    private static final Logger logger = LogManager.getLogger(DataReader.class);

    /**
     * Reads participant definitions from a file.
     *
     * @param filePath path to the participants configuration file
     * @return list of parsed {@link Participant} objects
     * @throws IOException if file reading fails
     */
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

    /**
     * Parses a single participant definition line.
     *
     * @param line raw configuration line
     * @return constructed {@link Participant} instance
     */
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

    /**
     * Container class holding all runtime configuration data.
     * <p>
     * Used to transfer initialization parameters to the application entry point.
     * </p>
     */
    public static class ConfigData {

        private final List<Participant> participants;
        private final int numberOfTransactionsPerParticipant;
        private final long reportIntervalMs;

        /**
         * Creates configuration data container.
         *
         * @param participants list of exchange participants
         * @param numberOfTransactionsPerParticipant number of transactions per participant
         * @param reportIntervalMs reporting interval in milliseconds
         */
        public ConfigData(List<Participant> participants,
                          int numberOfTransactionsPerParticipant,
                          long reportIntervalMs) {
            this.participants = participants;
            this.numberOfTransactionsPerParticipant = numberOfTransactionsPerParticipant;
            this.reportIntervalMs = reportIntervalMs;
        }

        /**
         * Returns list of participants.
         *
         * @return participants list
         */
        public List<Participant> getParticipants() {
            return participants;
        }

        /**
         * Returns number of transactions per participant.
         *
         * @return number of transactions
         */
        public int getNumberOfTransactionsPerParticipant() {
            return numberOfTransactionsPerParticipant;
        }

        /**
         * Returns balance reporting interval.
         *
         * @return interval in milliseconds
         */
        public long getReportIntervalMs() {
            return reportIntervalMs;
        }
    }

    /**
     * Reads application configuration using the provided participants file.
     * <p>
     * Currently initializes default runtime parameters and participant data.
     * </p>
     *
     * @param participantsFile path to participants configuration file
     * @return populated {@link ConfigData} instance
     * @throws IOException if file reading fails
     */
    public static ConfigData readConfig(String participantsFile) throws IOException {
        List<Participant> participants = readParticipants(participantsFile);

        int numberOfTransactionsPerParticipant = 10;
        long reportIntervalMs = 3000;

        return new ConfigData(participants, numberOfTransactionsPerParticipant, reportIntervalMs);
    }
}
