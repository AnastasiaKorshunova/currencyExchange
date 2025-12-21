package com.exchange;

import com.exchange.model.Participant;
import com.exchange.service.Exchange;
import com.exchange.thread.BalanceReporter;
import com.exchange.thread.ParticipantThread;
import com.exchange.util.DataReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Currency Exchange Application Starting...");

        String participantsFile = "data/participants.txt";
        if (args.length > 0) {
            participantsFile = args[0];
        }

        try {
            DataReader.ConfigData config = DataReader.readConfig(participantsFile);
            List<Participant> participants = config.getParticipants();
            int transactionsPerParticipant = config.getNumberOfTransactionsPerParticipant();
            long reportInterval = config.getReportIntervalMs();

            if (participants.isEmpty()) {
                logger.error("No participants loaded. Exiting.");
                return;
            }

            Exchange exchange = Exchange.getInstance();
            for (Participant participant : participants) {
                exchange.registerParticipant(participant);
            }

            BalanceReporter reporter = new BalanceReporter(reportInterval);
            Thread reporterThread = new Thread(reporter, "BalanceReporter");
            reporterThread.setDaemon(true);
            reporterThread.start();

            logger.info("Starting trading with {} participants, {} transactions each",
                participants.size(), transactionsPerParticipant);

            ExecutorService executorService = Executors.newFixedThreadPool(
                participants.size(),
                new ThreadFactory() {
                    private int counter = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("Participant-" + (++counter));
                        return thread;
                    }
                }
            );

            List<Future<String>> futures = new ArrayList<>();

            for (Participant participant : participants) {
                ParticipantThread task = new ParticipantThread(participant, transactionsPerParticipant);
                Future<String> future = executorService.submit(task);
                futures.add(future);
            }

            logger.info("All participant threads submitted. Waiting for completion...");

            for (Future<String> future : futures) {
                try {
                    String result = future.get();
                    logger.info(result);
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Error executing participant thread", e);
                }
            }

            executorService.shutdown();
            boolean terminated = executorService.awaitTermination(30, TimeUnit.SECONDS);

            if (!terminated) {
                logger.warn("Executor service did not terminate in time, forcing shutdown");
                executorService.shutdownNow();
            }

            TimeUnit.SECONDS.sleep(2);

            reporter.stop();
            while (reporter.isRunning() && reporterThread.isAlive()) {
                reporterThread.join(TimeUnit.SECONDS.toMillis(1));
            }

            logger.info("═".repeat(80));
            logger.info("FINAL SUMMARY");
            logger.info("═".repeat(80));
            logger.info("Total transactions executed: {}", exchange.getTransactionHistory().size());
            logger.info("Application completed successfully");

        } catch (Exception e) {
            logger.error("Fatal error in application", e);
            System.exit(1);
        }
    }
}
