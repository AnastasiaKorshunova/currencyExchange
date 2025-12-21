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

public class BalanceReporter implements Runnable {
    private static final Logger logger = LogManager.getLogger(BalanceReporter.class);

    private final Exchange exchange;
    private final long reportIntervalMs;
    private final AtomicBoolean running;

    public BalanceReporter(long reportIntervalMs) {
        this.exchange = Exchange.getInstance();
        this.reportIntervalMs = reportIntervalMs;
        this.running = new AtomicBoolean(true);
    }

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
                participant.getActiveTransactions() + "/" + participant.getMaxConcurrentTransactions()
            ));
        }

        report.append("═".repeat(120)).append("\n");

        printExchangeRates(report);

        report.append("═".repeat(120)).append("\n");

        System.out.println(report);
    }

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

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    public void stop() {
        running.set(false);
        logger.info("Balance Reporter stop requested");
    }

    public boolean isRunning() {
        return running.get();
    }
}
