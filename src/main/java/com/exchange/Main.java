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

/**
 * Application entry point.
 * <p>
 * Initializes the currency exchange system, loads participants,
 * starts participant trading threads and a balance reporting thread,
 * and waits for all tasks to complete.
 * </p>
 */
public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {

        logger.info("Currency Exchange Application Starting...");

        String participantsFile = args.length > 0
                ? args[0]
                : "data/participants.txt";

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
            participants.forEach(exchange::registerParticipant);

            BalanceReporter reporter = new BalanceReporter(reportInterval);
            Thread reporterThread = new Thread(reporter, "BalanceReporter");
            reporterThread.setDaemon(true);
            reporterThread.start();

            logger.info(
                    "Starting trading with {} participants, {} transactions each",
                    participants.size(), transactionsPerParticipant
            );

            ExecutorService executorService = Executors.newFixedThreadPool(
                    participants.size(),
                    new ThreadFactory() {
                        private int counter = 0;

                        @Override
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r);
                            t.setName("Participant-" + (++counter));
                            return t;
                        }
                    }
            );

            List<Future<String>> futures = new ArrayList<>();

            for (Participant participant : participants) {
                futures.add(
                        executorService.submit(
                                new ParticipantThread(participant, transactionsPerParticipant)
                        )
                );
            }

            logger.info("All participant threads submitted. Waiting for completion...");

            for (Future<String> future : futures) {
                try {
                    logger.info(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Error executing participant thread", e);
                }
            }

            executorService.shutdown();
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Executor service did not terminate in time, forcing shutdown");
                executorService.shutdownNow();
            }

            logger.info("═".repeat(80));
            logger.info("FINAL SUMMARY");
            logger.info("═".repeat(80));
            logger.info(
                    "Total transactions executed: {}",
                    exchange.getTransactionHistory().size()
            );
            logger.info("Application completed successfully");

        } catch (Exception e) {
            logger.error("Fatal error in application", e);
            System.exit(1);
        }
    }
}
