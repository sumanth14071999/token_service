#!/usr/bin/env bash
# Publishes one token event (JSON) to tokens.events on the docker-compose Kafka, keyed by its uid.
# Usage: ./scripts/send-token-event.sh '{"type":"TOKEN_BLOCKED","uid":"04A2B3C4D5E6F7","reason":"STOLEN"}'
set -euo pipefail

JSON="${1:?event JSON required}"
UID_KEY=$(echo "$JSON" | sed -E 's/.*"uid" *: *"([^"]+)".*/\1/')

docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic tokens.events \
  --property parse.key=true \
  --property key.separator='|' <<EOF
${UID_KEY}|${JSON}
EOF

echo "Sent to tokens.events (key ${UID_KEY}): ${JSON}"
