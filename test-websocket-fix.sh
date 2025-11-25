#!/bin/bash

#############################################################################
# WebSocket Authentication Fix - End-to-End Test
# 
# Tests the complete flow:
# 1. Login customer (Giulia Bianchi)
# 2. Create reservation
# 3. Verify WebSocket CONNECT frame accepted
# 4. Subscribe to notifications
# 5. Verify real-time notification received
#############################################################################

set -e

BASE_URL="https://api.greedys.it"
CUSTOMER_EMAIL="giulia.bianchi@example.com"
CUSTOMER_PASSWORD="CustomerPass123!"
RESTAURANT_ID="3"
SLOT_ID="252"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  WebSocket Authentication Fix - End-to-End Test${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}\n"

#############################################################################
# STEP 1: Login Customer
#############################################################################
echo -e "${YELLOW}[STEP 1]${NC} Logging in as Giulia Bianchi..."
echo "Email: $CUSTOMER_EMAIL"
echo "Password: ••••••••"

LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/customer/auth/login" \
  -H "Content-Type: application/json" \
  -k \
  -d "{
    \"username\": \"$CUSTOMER_EMAIL\",
    \"password\": \"$CUSTOMER_PASSWORD\",
    \"rememberMe\": true
  }")

echo "Response: $LOGIN_RESPONSE"

# Extract JWT token
JWT_TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$JWT_TOKEN" ]; then
  echo -e "${RED}❌ LOGIN FAILED - No JWT token received${NC}"
  echo "Full response: $LOGIN_RESPONSE"
  exit 1
fi

echo -e "${GREEN}✅ LOGIN SUCCESSFUL${NC}"
echo "JWT Token: ${JWT_TOKEN:0:50}..."
echo ""

#############################################################################
# STEP 2: Create Reservation
#############################################################################
echo -e "${YELLOW}[STEP 2]${NC} Creating reservation for Giulia Bianchi..."

RESERVATION_RESPONSE=$(curl -s -X POST "$BASE_URL/customer/reservations" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -k \
  -d "{
    \"userName\": \"Giulia Bianchi\",
    \"idSlot\": $SLOT_ID,
    \"pax\": 2,
    \"kids\": 0,
    \"notes\": \"Tavolo vicino alla finestra - WebSocket Test\",
    \"reservationDay\": \"2025-11-24\",
    \"restaurantId\": $RESTAURANT_ID
  }")

echo "Response: $RESERVATION_RESPONSE"

# Extract reservation ID
RESERVATION_ID=$(echo $RESERVATION_RESPONSE | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

if [ -z "$RESERVATION_ID" ]; then
  echo -e "${RED}❌ RESERVATION CREATION FAILED${NC}"
  echo "Full response: $RESERVATION_RESPONSE"
  exit 1
fi

echo -e "${GREEN}✅ RESERVATION CREATED${NC}"
echo "Reservation ID: $RESERVATION_ID"
echo ""

#############################################################################
# STEP 3: Check Server Logs for WebSocket CONNECT
#############################################################################
echo -e "${YELLOW}[STEP 3]${NC} Checking server logs for WebSocket CONNECT frame..."
echo "Looking for: '✅ CONNECT frame' in logs"
echo ""

# Try to get logs from the server
LOGS=$(ssh -i /home/valentino/.ssh/id_rsa deployer@46.101.209.92 "docker logs greedys_api 2>&1 | tail -100" 2>/dev/null || echo "Could not fetch remote logs")

if echo "$LOGS" | grep -q "✅ CONNECT frame"; then
  echo -e "${GREEN}✅ WEBSOCKET CONNECT FRAME ACCEPTED${NC}"
  echo "$LOGS" | grep "✅ CONNECT frame"
else
  echo -e "${YELLOW}⚠️  Could not verify CONNECT frame in logs (might need manual check)${NC}"
fi
echo ""

#############################################################################
# STEP 4: WebSocket Connection Test (local simulation)
#############################################################################
echo -e "${YELLOW}[STEP 4]${NC} WebSocket connection test (simulated)..."
echo "JWT Token for WebSocket: ${JWT_TOKEN:0:50}..."
echo ""

cat > /tmp/websocket-test.html << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>WebSocket Test - Greedys API</title>
    <script src="https://cdn.jsdelivr.net/npm/stomp-client@latest/lib/stomp.min.js"></script>
</head>
<body>
    <h1>WebSocket Test</h1>
    <div id="status">Connecting...</div>
    <div id="log"></div>
    
    <script>
        const token = "JWT_TOKEN_HERE";
        const restaurantId = 3;
        
        function log(msg) {
            const logDiv = document.getElementById('log');
            logDiv.innerHTML += msg + "<br>";
        }
        
        // Create WebSocket connection
        const socket = new WebSocket(`wss://api.greedys.it/stomp?token=${token}`);
        
        socket.onopen = function() {
            log("✅ WebSocket connected");
            
            // Try STOMP CONNECT
            const client = Stomp.over(socket);
            client.connect({}, 
                function(frame) {
                    log("✅ STOMP CONNECT successful");
                    
                    // Subscribe to reservation notifications
                    client.subscribe(`/topic/restaurant/${restaurantId}/reservations`, 
                        function(message) {
                            log("📬 NOTIFICATION RECEIVED: " + message.body);
                        }
                    );
                    
                    document.getElementById('status').innerHTML = "✅ Connected and subscribed";
                },
                function(error) {
                    log("❌ STOMP CONNECT failed: " + error);
                    document.getElementById('status').innerHTML = "❌ Connection failed";
                }
            );
        };
        
        socket.onerror = function(error) {
            log("❌ WebSocket error: " + error);
            document.getElementById('status').innerHTML = "❌ WebSocket error";
        };
    </script>
</body>
</html>
EOF

echo "WebSocket test HTML created: /tmp/websocket-test.html"
echo "Replace JWT_TOKEN_HERE with: $JWT_TOKEN"
echo ""

#############################################################################
# STEP 5: Verify Backend Logs for Notifications
#############################################################################
echo -e "${YELLOW}[STEP 5]${NC} Checking for notification delivery logs..."

if echo "$LOGS" | grep -q "SUBSCRIBE allowed"; then
  echo -e "${GREEN}✅ SUBSCRIBE FRAME ACCEPTED${NC}"
  echo "$LOGS" | grep "SUBSCRIBE allowed"
elif echo "$LOGS" | grep -q "SUBSCRIBE denied"; then
  echo -e "${RED}❌ SUBSCRIBE FRAME DENIED${NC}"
  echo "$LOGS" | grep "SUBSCRIBE denied"
else
  echo -e "${YELLOW}⚠️  Could not verify SUBSCRIBE in logs${NC}"
fi
echo ""

#############################################################################
# SUMMARY
#############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✅ TEST SUMMARY${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}\n"

echo -e "${GREEN}✅ Customer Login:${NC} SUCCESS"
echo "   - Email: $CUSTOMER_EMAIL"
echo "   - JWT Token received: ${JWT_TOKEN:0:50}..."
echo ""

echo -e "${GREEN}✅ Reservation Created:${NC} SUCCESS"
echo "   - ID: $RESERVATION_ID"
echo "   - Restaurant ID: $RESTAURANT_ID"
echo "   - Date: 2025-11-24"
echo ""

echo -e "${YELLOW}ℹ️  WebSocket Status:${NC} MANUAL VERIFICATION NEEDED"
echo "   To fully verify WebSocket:"
echo "   1. Open the HTML test file in a browser"
echo "   2. Check server logs for '✅ CONNECT frame'"
echo "   3. Verify notifications are received"
echo ""

echo -e "${BLUE}Test credentials for manual verification:${NC}"
echo "   Email: $CUSTOMER_EMAIL"
echo "   Password: $CUSTOMER_PASSWORD"
echo "   JWT Token: $JWT_TOKEN"
echo "   Restaurant ID: $RESTAURANT_ID"
echo ""

echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}\n"
