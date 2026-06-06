package com.example.functions.common;

public final class Env {
    private Env() {}
    public static String get(String k) { return System.getenv(k); }
    public static String get(String k, String def) {
        String v = System.getenv(k);
        return v == null || v.isBlank() ? def : v;
    }
}
