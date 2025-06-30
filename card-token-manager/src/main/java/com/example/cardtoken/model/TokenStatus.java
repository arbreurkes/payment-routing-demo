package com.example.cardtoken.model;

/** Represents the status of a card token. */
public enum TokenStatus {
    /** Token is active and can be used for transactions */
    ACTIVE,

    /** Token is suspended and cannot be used until reactivated */
    SUSPENDED,

    /** Token has expired and cannot be used */
    EXPIRED,

    /** Token has been deleted/invalidated */
    DELETED
}
