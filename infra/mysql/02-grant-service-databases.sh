#!/bin/sh
# MySQL image entrypoint sources non-executable .sh init files with env loaded.
set -eu
mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL
GRANT ALL PRIVILEGES ON user_db.* TO '$MYSQL_USER'@'%';
GRANT ALL PRIVILEGES ON shipment_db.* TO '$MYSQL_USER'@'%';
GRANT ALL PRIVILEGES ON tracking_db.* TO '$MYSQL_USER'@'%';
FLUSH PRIVILEGES;
SQL
