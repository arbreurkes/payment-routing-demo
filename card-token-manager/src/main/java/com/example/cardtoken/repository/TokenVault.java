package com.example.cardtoken.repository;

import com.example.cardtoken.model.CardToken;

import java.util.Optional;

/** Interface for a token vault that stores and retrieves card tokens. */
public interface TokenVault {

    /**
     * Saves a token to the vault.
     *
     * @param token The token to save
     * @return The saved token
     */
    CardToken save(CardToken token);

    /**
     * Finds a token by its reference.
     *
     * @param tokenReference The token reference
     * @return An Optional containing the token if found
     */
    Optional<CardToken> findByTokenReference(String tokenReference);

    /**
     * Finds a token by its value.
     *
     * @param tokenValue The token value
     * @return An Optional containing the token if found
     */
    Optional<CardToken> findByTokenValue(String tokenValue);

    /**
     * Checks if a token exists by its reference.
     *
     * @param tokenReference The token reference
     * @return true if the token exists, false otherwise
     */
    boolean existsByTokenReference(String tokenReference);

    /**
     * Checks if a token exists by its value.
     *
     * @param tokenValue The token value
     * @return true if the token exists, false otherwise
     */
    boolean existsByTokenValue(String tokenValue);

    /**
     * Deletes a token by its reference.
     *
     * @param tokenReference The token reference
     */
    void deleteByTokenReference(String tokenReference);

    /** Clears all tokens from the vault. */
    void clear();
}
