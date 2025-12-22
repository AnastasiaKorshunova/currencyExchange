package com.exchange.model;

/**
 * Represents supported currencies with their full names and symbols.
 */
public enum Currency {
    MNT("Mongolian Tugrik", "₮"),
    USD("US Dollar", "$"),
    EUR("Euro", "€");

    private final String fullName;
    private final String symbol;

    /**
     * Creates a currency entry.
     *
     * @param fullName descriptive name of the currency
     * @param symbol   printable symbol associated with the currency
     */
    Currency(String fullName, String symbol) {
        this.fullName = fullName;
        this.symbol = symbol;
    }

    /**
     * Returns the descriptive name of the currency.
     *
     * @return full currency name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Returns the printable symbol for the currency.
     *
     * @return currency symbol
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Returns a formatted representation combining the currency code and symbol.
     *
     * @return string representation of the currency
     */
    @Override
    public String toString() {
        return name() + " (" + symbol + ")";
    }
}