package com.example.cardtoken.repository;

import com.example.cardtoken.model.CardToken;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** An in-memory implementation of a token vault for storing and retrieving card tokens. */
@Repository
public class InMemoryTokenVault implements TokenVault {

    // Thread-safe map to store tokens by their reference
    private final Map<String, CardToken> tokensByReference = new ConcurrentHashMap<>();

    // Thread-safe map to store tokens by their token value
    private final Map<String, CardToken> tokensByValue = new ConcurrentHashMap<>();

    @Override
    public CardToken save(CardToken token) {
        // Store token in all maps for different lookup methods
        tokensByReference.put(token.getTokenReference(), token);
        tokensByValue.put(token.getTokenValue(), token);
        return token;
    }

    @Override
    public Optional<CardToken> findByTokenReference(String tokenReference) {
        return Optional.ofNullable(tokensByReference.get(tokenReference));
    }

    @Override
    public Optional<CardToken> findByTokenValue(String tokenValue) {
        return Optional.ofNullable(tokensByValue.get(tokenValue));
    }

    @Override
    public boolean existsByTokenReference(String tokenReference) {
        return tokensByReference.containsKey(tokenReference);
    }

    @Override
    public boolean existsByTokenValue(String tokenValue) {
        return tokensByValue.containsKey(tokenValue);
    }

    @Override
    public void deleteByTokenReference(String tokenReference) {
        CardToken token = tokensByReference.remove(tokenReference);
        if (token != null) {
            tokensByValue.remove(token.getTokenValue());
        }
    }

    @Override
    public void clear() {
        tokensByReference.clear();
        tokensByValue.clear();
    }
}
