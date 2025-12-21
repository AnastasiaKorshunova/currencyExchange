package com.exchange.model;

public enum Currency {
    MNT("Mongolian Tugrik", "₮"),
    USD("US Dollar", "$"),
    EUR("Euro", "€");

    private final String fullName;
    private final String symbol;

    Currency(String fullName, String symbol) {
        this.fullName = fullName;
        this.symbol = symbol;
    }

    public String getFullName() {
        return fullName;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return name() + " (" + symbol + ")";
    }
}
