#!/bin/bash

# Integration test script for Payments Ledger API
# Prerequisites: API server running at http://localhost:8000

set -e

echo "=== Payments Ledger API Integration Test ==="
echo ""

BASE_URL="http://localhost:8000"

echo "1. Creating Account A with initial balance $1000..."
ACC_A=$(curl -s -X POST "$BASE_URL/accounts" \
  -H "Content-Type: application/json" \
  -d '{"initial_balance": 1000}' | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")
echo "   Account A ID: $ACC_A"

echo ""
echo "2. Creating Account B with initial balance $500..."
ACC_B=$(curl -s -X POST "$BASE_URL/accounts" \
  -H "Content-Type: application/json" \
  -d '{"initial_balance": 500}' | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")
echo "   Account B ID: $ACC_B"

echo ""
echo "3. Checking initial balances..."
BALANCE_A=$(curl -s "$BASE_URL/accounts/$ACC_A" | python3 -c "import sys, json; print(json.load(sys.stdin)['balance'])")
BALANCE_B=$(curl -s "$BASE_URL/accounts/$ACC_B" | python3 -c "import sys, json; print(json.load(sys.stdin)['balance'])")
echo "   Account A: \$$BALANCE_A"
echo "   Account B: \$$BALANCE_B"

echo ""
echo "4. Transferring \$300 from A to B..."
TX_RESPONSE=$(curl -s -X POST "$BASE_URL/transactions" \
  -H "Content-Type: application/json" \
  -d "{
    \"from_account_id\": \"$ACC_A\",
    \"to_account_id\": \"$ACC_B\",
    \"amount\": 300
  }")
echo "   Transaction: $(echo $TX_RESPONSE | python3 -c "import sys, json; data=json.load(sys.stdin); print(f\"ID={data['id']}, Status={data['status']}\")")"

echo ""
echo "5. Checking balances after transfer..."
BALANCE_A=$(curl -s "$BASE_URL/accounts/$ACC_A" | python3 -c "import sys, json; print(json.load(sys.stdin)['balance'])")
BALANCE_B=$(curl -s "$BASE_URL/accounts/$ACC_B" | python3 -c "import sys, json; print(json.load(sys.stdin)['balance'])")
echo "   Account A: \$$BALANCE_A (expected: 700.00)"
echo "   Account B: \$$BALANCE_B (expected: 800.00)"

echo ""
echo "6. Attempting insufficient funds transfer (should fail)..."
ERROR_RESPONSE=$(curl -s -X POST "$BASE_URL/transactions" \
  -H "Content-Type: application/json" \
  -d "{
    \"from_account_id\": \"$ACC_A\",
    \"to_account_id\": \"$ACC_B\",
    \"amount\": 10000
  }")
echo "   Error: $(echo $ERROR_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin).get('detail', 'Success'))")"

echo ""
echo "7. Verifying balances unchanged after failed transfer..."
BALANCE_A=$(curl -s "$BASE_URL/accounts/$ACC_A" | python3 -c "import sys, json; print(json.load(sys.stdin)['balance'])")
BALANCE_B=$(curl -s "$BASE_URL/accounts/$ACC_B" | python3 -c "import sys, json; print(json.load(sys.stdin)['balance'])")
echo "   Account A: \$$BALANCE_A (expected: 700.00)"
echo "   Account B: \$$BALANCE_B (expected: 800.00)"

echo ""
echo "=== Integration Test Complete ==="
echo "Total money in system: \$$(python3 -c "print(float($BALANCE_A) + float($BALANCE_B))")"
echo "Expected: \$1500.00"
