#!/bin/bash
set -e

psql -U "$POSTGRES_USER" -c "CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD 'pass';"

echo "host replication replicator 0.0.0.0/0 md5" >> "$PGDATA/pg_hba.conf"

pg_ctl reload -D "$PGDATA"
