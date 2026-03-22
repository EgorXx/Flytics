#!/bin/bash
set -e

echo "Waiting for master..."
until pg_isready -h postgres-master -p 5432 -U postgres; do
  sleep 2
done

rm -rf "$PGDATA"/*

PGPASSWORD=pass pg_basebackup \
  -h postgres-master \
  -D "$PGDATA" \
  -U replicator \
  -P \
  -R

exec docker-entrypoint.sh postgres