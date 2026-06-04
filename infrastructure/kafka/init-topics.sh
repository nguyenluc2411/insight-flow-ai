#!/bin/bash
# Creates all required Kafka topics for Insight Flow AI.
# Run via kafka-init container in docker-compose.yml, or manually:
#   docker exec insight-kafka bash /scripts/init-topics.sh
set -e

BROKER="${KAFKA_BROKER:-kafka:29092}"
PARTITIONS="${KAFKA_PARTITIONS:-3}"
RF="${KAFKA_REPLICATION_FACTOR:-1}"

create_topic() {
  local topic=$1
  kafka-topics \
    --bootstrap-server "$BROKER" \
    --create \
    --if-not-exists \
    --topic "$topic" \
    --partitions "$PARTITIONS" \
    --replication-factor "$RF"
  echo "  ✓ $topic"
}

echo "Creating Insight Flow AI Kafka topics on $BROKER ..."

# Catalog events
create_topic catalog.product.created
create_topic catalog.inventory.updated

# Sales events
create_topic sales.order.completed

# Integration events
create_topic integration.sync.completed

# ML events
create_topic ml.forecast.generated
create_topic ml.recommendation.created

# Inventory ingestion events (file upload → parse → recommend)
create_topic inventory.file.uploaded
create_topic inventory.ingestion.completed
create_topic inventory.ingestion.failed
create_topic inventory.recommendation.generated

echo "All topics created."
kafka-topics --bootstrap-server "$BROKER" --list
