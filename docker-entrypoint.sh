#!/bin/bash
# Starts MySQL + the banking API in one container (Render free web service).
set -e

export DB_HOST="${DB_HOST:-127.0.0.1}"
export DB_PORT="${DB_PORT:-3306}"
export DB_NAME="${DB_NAME:-bankdb}"
export DB_USER="${DB_USER:-bankuser}"
export DB_PASSWORD="${DB_PASSWORD:-bankpass}"

DATADIR=/var/lib/mysql
if [ ! -d "$DATADIR/mysql" ]; then
  echo "Initializing MySQL data directory..."
  mysqld --initialize-insecure --user=mysql --datadir="$DATADIR" > /tmp/mysql-init.log 2>&1
fi

mysqld --user=mysql > /tmp/mysqld.log 2>&1 &

echo "Waiting for MySQL..."
for i in $(seq 1 40); do
  if mysqladmin --silent ping 2>/dev/null; then
    break
  fi
  sleep 2
done
mysqladmin ping

mysql -u root -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME};"
mysql -u root -e "CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED WITH mysql_native_password BY '${DB_PASSWORD}'; GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'%'; FLUSH PRIVILEGES;"

echo "Starting banking API..."
exec java -Xmx192m -cp "out:lib/*" WebMain "${PORT:-8080}"
