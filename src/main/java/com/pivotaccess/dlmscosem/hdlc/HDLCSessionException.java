package com.pivotaccess.dlmscosem.hdlc;

/**
 * Thrown when an HDLC session-level error occurs — link establishment failure,
 * timeout, sequence error, unexpected frame type, or DM response.
 */
public class HDLCSessionException extends Exception {

    public HDLCSessionException(String message) {
        super(message);
    }

    public HDLCSessionException(String message, Throwable cause) {
        super(message, cause);
    }
}