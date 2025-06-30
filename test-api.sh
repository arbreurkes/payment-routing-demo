#!/bin/bash

# Set the base URL
BASE_URL="http://localhost:8081/payment-gateway/api/v1/payments"
MERCHANT_ID="test-merchant-123"

echo "=== Testing Card Tokenization ==="
TOKEN_RESPONSE=$(curl -s -X POST "$BASE_URL/tokenize" \
  -H "Content-Type: application/json" \
  -H "X-Merchant-Id: $MERCHANT_ID" \
  -d '{
    "cardNumber": "4500123456789010",
    "cardholderName": "John Doe",
    "expiryMonth": 12,
    "expiryYear": 2030,
    "cvv": "123"
  }')

echo "Tokenization Response:"
echo $TOKEN_RESPONSE | jq .

# Extract token reference from response
TOKEN_REFERENCE=$(echo $TOKEN_RESPONSE | jq -r '.tokenReference')
echo "Token Reference: $TOKEN_REFERENCE"

echo -e "\n=== Testing Token Retrieval ==="
RETRIEVE_RESPONSE=$(curl -s -X GET "$BASE_URL/tokens/$TOKEN_REFERENCE" \
  -H "X-Merchant-Id: $MERCHANT_ID")

echo "Token Retrieval Response:"
echo $RETRIEVE_RESPONSE | jq .

echo -e "\n=== Testing Payment with Card Details ==="
CARD_PAYMENT_RESPONSE=$(curl -s -X POST "$BASE_URL/authorize" \
  -H "Content-Type: application/json" \
  -H "X-Merchant-Id: $MERCHANT_ID" \
  -d '{
    "merchantReference": "test-payment-'$(date +%s)'",
    "amount": 100.00,
    "currency": "USD",
    "cardDetails": {
      "cardNumber": "4500123456789010",
      "cardholderName": "John Doe",
      "expiryMonth": 12,
      "expiryYear": 2030,
      "cvv": "123"
    }
  }')

echo "Card Payment Response:"
echo $CARD_PAYMENT_RESPONSE | jq .

# Extract payment ID
CARD_PAYMENT_ID=$(echo $CARD_PAYMENT_RESPONSE | jq -r '.paymentId')
echo "Payment ID from card payment: $CARD_PAYMENT_ID"

echo -e "\n=== Testing Payment with Invalid Card Number ==="
INVALID_CARD_RESPONSE=$(curl -s -X POST "$BASE_URL/authorize" \
  -H "Content-Type: application/json" \
  -H "X-Merchant-Id: $MERCHANT_ID" \
  -d '{
    "merchantReference": "invalid-card-'$(date +%s)'",
    "amount": 150.00,
    "currency": "USD",
    "cardDetails": {
      "cardNumber": "1234567890123456",
      "cardholderName": "Jane Doe",
      "expiryMonth": 12,
      "expiryYear": 2030,
      "cvv": "456"
    }
  }')

echo "Invalid Card Payment Response:"
echo $INVALID_CARD_RESPONSE | jq .

echo -e "\n=== Testing Payment with Token ==="
TOKEN_PAYMENT_RESPONSE=$(curl -s -X POST "$BASE_URL/authorize" \
  -H "Content-Type: application/json" \
  -H "X-Merchant-Id: $MERCHANT_ID" \
  -d '{
    "merchantReference": "token-payment-'$(date +%s)'",
    "amount": 200.00,
    "currency": "USD",
    "tokenReference": "'$TOKEN_REFERENCE'"
  }')

echo "Token Payment Response:"
echo $TOKEN_PAYMENT_RESPONSE | jq .

# Extract payment ID
TOKEN_PAYMENT_ID=$(echo $TOKEN_PAYMENT_RESPONSE | jq -r '.paymentId')
echo "Payment ID from token payment: $TOKEN_PAYMENT_ID"

echo -e "\n=== Testing Payment Capture ==="
if [ -n "$TOKEN_PAYMENT_ID" ] && [ "$TOKEN_PAYMENT_ID" != "null" ]; then
  CAPTURE_RESPONSE=$(curl -s -X POST "$BASE_URL/$TOKEN_PAYMENT_ID/capture" \
    -H "X-Merchant-Id: $MERCHANT_ID")
  
  echo "Capture Response:"
  echo $CAPTURE_RESPONSE | jq .
else
  echo "Skipping capture test because no valid payment ID was obtained"
fi

# If we got a valid card payment ID, try to capture that too
if [ -n "$CARD_PAYMENT_ID" ] && [ "$CARD_PAYMENT_ID" != "null" ]; then
  echo -e "\n=== Testing Card Payment Capture ==="
  CARD_CAPTURE_RESPONSE=$(curl -s -X POST "$BASE_URL/$CARD_PAYMENT_ID/capture" \
    -H "X-Merchant-Id: $MERCHANT_ID")
  
  echo "Card Capture Response:"
  echo $CARD_CAPTURE_RESPONSE | jq .
fi
