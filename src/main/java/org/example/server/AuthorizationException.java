package org.example.server;

public class AuthorizationException extends Exception {
    private final int statusCode;
    private final String challenge;

    public AuthorizationException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.challenge = null;
    }

    public AuthorizationException(int statusCode, String message, String challenge) {
        super(message);
        this.statusCode = statusCode;
        this.challenge = challenge;
    }

    public AuthorizationException(int statusCode, String message, Throwable t) {
        super(message, t);
        this.statusCode = statusCode;
        this.challenge = null;
    }

    public AuthorizationException(int statusCode, String message, String challenge, Throwable t) {
        super(message, t);
        this.statusCode = statusCode;
        this.challenge = challenge;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getChallenge() {
        return challenge;
    }
}
