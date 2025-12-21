package com.exchange.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

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

    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        REJECTED
    }

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

    public long getId() {
        return id;
    }

    public Participant getBuyer() {
        return buyer;
    }

    public Participant getSeller() {
        return seller;
    }

    public Currency getFromCurrency() {
        return fromCurrency;
    }

    public Currency getToCurrency() {
        return toCurrency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public BigDecimal getConvertedAmount() {
        return amount.multiply(rate);
    }

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

    public static class Builder {
        private Participant buyer;
        private Participant seller;
        private Currency fromCurrency;
        private Currency toCurrency;
        private BigDecimal amount;
        private BigDecimal rate;
        private TransactionStatus status = TransactionStatus.PENDING;

        public Builder buyer(Participant buyer) {
            this.buyer = buyer;
            return this;
        }

        public Builder seller(Participant seller) {
            this.seller = seller;
            return this;
        }

        public Builder fromCurrency(Currency from) {
            this.fromCurrency = from;
            return this;
        }

        public Builder toCurrency(Currency to) {
            this.toCurrency = to;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder rate(BigDecimal rate) {
            this.rate = rate;
            return this;
        }

        public Builder status(TransactionStatus status) {
            this.status = status;
            return this;
        }

        public Transaction build() {
            return new Transaction(this);
        }
    }
}
