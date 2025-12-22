package com.exchange.thread;

import com.exchange.model.Currency;
import com.exchange.model.Participant;
import com.exchange.service.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Periodic balance reporting task.
 * <p>
 * {@code BalanceReporter} is a dedicated monitoring thread responsible
 * for periodically printing the current balance of all exchange
 * participants along with active transaction counts and exchange rates.
 * </p>
 *
 * <p>
 * This class does not modify any shared data and operates in a read-only
 * manner, making it safe to run concurrently with trading threads.
 * </p>
 *
 * <p>
 * The reporting loop can be safely started and stopped using
 * an {@link AtomicBoolean} without blocking or explicit synchronization.
 * </p>
 */
public class BalanceReporter implements Runnable {

    private static final Logger logger = LogManager.getLogger(BalanceReporter.class);

    /** Shared exchange instance used to retrieve participants and rates. */
    private final Exchange exchange;

    /** Interval in milliseconds between balance reports. */
    private final long reportIntervalMs;

    /** Flag controlling execution of the reporting loop. */
    private final AtomicBoolean running;

    /**
     * Creates a new balance reporter with the specified reporting interval.
     *
     * @param reportIntervalMs interval between reports in milliseconds
     */
    public BalanceReporter(long reportIntervalMs) {
        this.exchange = Exchange.getInstance();
        this.reportIntervalMs = reportIntervalMs;
        this.running = new AtomicBoolean(true);
    }

    /**
     * Main execution loop of the reporting thread.
     * <p>
     * Periodically prints balance reports while the reporter is running.
     * The loop can be interrupted or stopped externally.
     * </p>
     */
    @Override
    public void run() {
        logger.info("Balance Reporter started, reporting every {} ms", reportIntervalMs);

        try {
            while (running.get()) {
                printBalanceReport();
                TimeUnit.MILLISECONDS.sleep(reportIntervalMs);
            }
        } catch (InterruptedException e) {
            logger.info("Balance Reporter interrupted");
            Thread.currentThread().interrupt();
        }

        logger.info("Balance Reporter stopped");
    }

    /**
     * Builds and prints a formatted balance report for all participants.
     */
    private void printBalanceReport() {
        List<Participant> participants = exchange.getParticipants();

        if (participants.isEmpty()) {
            logger.info("No participants registered yet");
            return;
        }

        StringBuilder report = new StringBuilder();
        report.append("\n");
        report.append("═".repeat(120)).append("\n");
        report.append("                              BALANCE REPORT\n");
        report.append("═".repeat(120)).append("\n");

        String headerFormat = "%-5s %-25s %-15s %-18s %-18s %-18s %-15s\n";
        report.append(String.format(headerFormat,
                "ID", "Name", "State", "MNT (₮)", "USD ($)", "EUR (€)", "Active Txns"));
        report.append("─".repeat(120)).append("\n");

        String rowFormat = "%-5d %-25s %-15s %,18.2f %,18.2f %,18.2f %-15s\n";

        for (Participant participant : participants) {
            Map<Currency, BigDecimal> balance = participant.getBalance().getSnapshot();

            report.append(String.format(rowFormat,
                    participant.getId(),
                    truncate(participant.getName(), 25),
                    participant.getState().getStateName(),
                    balance.get(Currency.MNT),
                    balance.get(Currency.USD),
                    balance.get(Currency.EUR),
                    participant.getActiveTransactions() + "/" +
                            participant.getMaxConcurrentTransactions()
            ));
        }

        report.append("═".repeat(120)).append("\n");

        printExchangeRates(report);

        report.append("═".repeat(120)).append("\n");

        System.out.println(report);
    }

    /**
     * Appends current exchange rates to the balance report.
     *
     * @param report report builder
     */
    private void printExchangeRates(StringBuilder report) {
        Map<String, BigDecimal> rates = exchange.getAllRates();

        if (!rates.isEmpty()) {
            report.append("Exchange Rates: ");

            rates.forEach((key, rate) -> {
                String[] currencies = key.split("_");
                if (currencies.length == 2) {
                    report.append(String.format("%s→%s: %.4f  ",
                            currencies[0], currencies[1], rate));
                }
            });

            report.append("\n");
        }
    }

    /**
     * Truncates a string to a maximum length with ellipsis.
     *
     * @param text input string
     * @param maxLength maximum allowed length
     * @return truncated string
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Requests graceful termination of the reporter thread.
     */
    public void stop() {
        running.set(false);
        logger.info("Balance Reporter stop requested");
    }

    /**
     * Checks whether the reporter is currently running.
     *
     * @return {@code true} if running, {@code false} otherwise
     */
    public boolean isRunning() {
        return running.get();
    }
}
