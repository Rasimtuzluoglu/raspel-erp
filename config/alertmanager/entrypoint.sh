#!/bin/sh
set -e

# Alertmanager ortam degiskeni genisletmeyi desteklemedigi icin bu script
# Slack webhook'unu konfigurasyona yerlestirir. SLACK_WEBHOOK_URL bos ise
# Slack blogu kaldirilir (alertler yalnizca web arayuzunde gorunur).

CONFIG="/etc/alertmanager/alertmanager.yml"
cp /etc/alertmanager/alertmanager.yml.tmpl "$CONFIG"

if [ -n "$SLACK_WEBHOOK_URL" ]; then
  sed -i "s|__SLACK_WEBHOOK_URL__|${SLACK_WEBHOOK_URL}|g" "$CONFIG"
else
  sed -i '/slack_configs:/,/send_resolved: true/d' "$CONFIG"
fi

exec /bin/alertmanager "$@"
