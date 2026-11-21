package com.xiying.shop.model;

public enum Currency {
    MONEY("金币"),
    POINTS("曦荧币");

    private final String label;

    Currency(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Currency parse(String raw) {
        if (raw == null) return MONEY;
        return raw.equalsIgnoreCase("POINTS") ? POINTS : MONEY;
    }
}
