package com.exchange.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a currency exchange transaction between a buyer and a seller.
 * <p>
 * A transaction is created through the {@link Builder} to enforce immutability of
 * its fields. Each instance is assigned a monotonically increasing identifier and
 * is timestamped at creation. Instances are intentionally immutable to ensure
 * thread-safety when used alongside the static {@link AtomicLong} id generator.
 */
public class Transaction {
    private static final AtomicLong idGenerator = new AtomicLong(0);

    private final long id;
    private final Participant buyer;
    private final Participant seller;
    private final Currency fromCurrency;
    private final Currency toCurrency;
    private final BigDecimal amount;
    private final BigDecimal rate;
    private final LocalDateTime timestamp;
    private final TransactionStatus status;

    /**
     * Represents the lifecycle state of a {@link Transaction}.
     */
    public enum TransactionStatus {
        /** Transaction has been created but not yet finalized. */
        PENDING,
        /** Transaction completed successfully. */
        COMPLETED,
        /** Transaction was rejected or failed. */
        REJECTED
    }

    /**
     * Builds a transaction from the supplied {@link Builder} instance, assigning a
     * unique identifier and timestamp at the moment of construction.
     *
     * @param builder builder carrying all required state
     */
    private Transaction(Builder builder) {
        this.id = idGenerator.incrementAndGet();
        this.buyer = builder.buyer;
        this.seller = builder.seller;
        this.fromCurrency = builder.fromCurrency;
        this.toCurrency = builder.toCurrency;
        this.amount = builder.amount;
        this.rate = builder.rate;
        this.timestamp = LocalDateTime.now();
        this.status = builder.status;
    }

    /**
     * Returns the unique identifier assigned to this transaction.
     *
     * @return monotonically increasing transaction id
     */
    public long getId() {
        return id;
    }

    /**
     * Returns the buyer participating in the transaction.
     *
     * @return buyer participant or {@code null} if not set
     */
    public Participant getBuyer() {
        return buyer;
    }

    /**
     * Returns the seller participating in the transaction.
     *
     * @return seller participant or {@code null} if not set
     */
    public Participant getSeller() {
        return seller;
    }

    /**
     * Returns the currency being exchanged from the buyer.
     *
     * @return originating currency
     */
    public Currency getFromCurrency() {
        return fromCurrency;
    }

    /**
     * Returns the currency expected by the seller.
     *
     * @return target currency
     */
    public Currency getToCurrency() {
        return toCurrency;
    }

    /**
     * Returns the amount of {@link #getFromCurrency() fromCurrency} to be converted.
     *
     * @return amount to convert
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Returns the conversion rate applied to the transaction.
     *
     * @return exchange rate
     */
    public BigDecimal getRate() {
        return rate;
    }

    /**
     * Returns the timestamp when the transaction was instantiated.
     *
     * @return creation timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the current status of the transaction.
     *
     * @return transaction status
     */
    public TransactionStatus getStatus() {
        return status;
    }

    /**
     * Calculates the converted amount using {@link #getAmount()} and {@link #getRate()}.
     *
     * @return resulting amount in {@link #getToCurrency() toCurrency}
     */
    public BigDecimal getConvertedAmount() {
        return amount.multiply(rate);
    }

    /**
     * Returns a human-readable summary of the transaction state.
     *
     * @return formatted transaction description
     */
    @Override
    public String toString() {
        return String.format("Transaction{id=%d, buyer=%s, seller=%s, %s->%s, amount=%.2f, rate=%.4f, status=%s}",
                id,
                buyer != null ? buyer.getName() : "N/A",
                seller != null ? seller.getName() : "N/A",
                fromCurrency,
                toCurrency,
                amount,
                rate,
                status);
    }

    /**
     * Fluent builder for creating immutable {@link Transaction} instances.
     */
    public static class Builder {
        private Participant buyer;
        private Participant seller;
        private Currency fromCurrency;
        private Currency toCurrency;
        private BigDecimal amount;
        private BigDecimal rate;
        private TransactionStatus status = TransactionStatus.PENDING;

        /**
         * Sets the buyer participant.
         *
         * @param buyer participant initiating the purchase
         * @return this builder instance
         */
        public Builder buyer(Participant buyer) {
            this.buyer = buyer;
            return this;
        }

        /**
         * Sets the seller participant.
         *
         * @param seller participant providing the currency
         * @return this builder instance
         */
        public Builder seller(Participant seller) {
            this.seller = seller;
            return this;
        }

        /**
         * Defines the currency being exchanged from the buyer.
         *
         * @param from currency provided by the buyer
         * @return this builder instance
         */
        public Builder fromCurrency(Currency from) {
            this.fromCurrency = from;
            return this;
        }

        /**
         * Defines the currency expected by the seller.
         *
         * @param to currency requested from the seller
         * @return this builder instance
         */
        public Builder toCurrency(Currency to) {
            this.toCurrency = to;
            return this;
        }

        /**
         * Sets the amount to be exchanged.
         *
         * @param amount amount in {@link #fromCurrency}
         * @return this builder instance
         */
        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        /**
         * Sets the exchange rate to apply to the transaction.
         *
         * @param rate conversion rate between currencies
         * @return this builder instance
         */
        public Builder rate(BigDecimal rate) {
            this.rate = rate;
            return this;
        }

        /**
         * Sets the initial status of the transaction.
         *
         * @param status transaction status
         * @return this builder instance
         */
        public Builder status(TransactionStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Builds a new {@link Transaction} with the configured properties.
         *
         * @return immutable transaction instance
         */
        public Transaction build() {
            return new Transaction(this);
        }
    }
}