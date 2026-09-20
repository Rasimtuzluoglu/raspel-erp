#!/bin/sh
set -e

# Alertmanager ortam degiskeni genisletmeyi desteklemedigi icin bu script
# bildirim kanallarini konfigurasyona yerlestirir. Tanimli olmayan kanalin
# blogu kaldirilir (alertler o web arayuzunde gorunmeye devam eder).

CONFIG="/etc/alertmanager/alertmanager.yml"
cp /etc/alertmanager/alertmanager.yml.tmpl "$CONFIG"

# Slack
if [ -n "$SLACK_WEBHOOK_URL" ]; then
  sed -i "s|__SLACK_WEBHOOK_URL__|${SLACK_WEBHOOK_URL}|g" "$CONFIG"
else
  sed -i '/slack_configs:/,/send_resolved: true/d' "$CONFIG"
fi

# E-posta (SMTP)
if [ -n "$ALERT_EMAIL_TO" ] && [ -n "$SMTP_SMARTHOST" ]; then
  sed -i "s|__SMTP_SMARTHOST__|${SMTP_SMARTHOST}|g" "$CONFIG"
  sed -i "s|__SMTP_FROM__|${SMTP_FROM:-alertmanager@raspel-erp.com}|g" "$CONFIG"
  sed -i "s|__SMTP_USERNAME__|${SMTP_USERNAME:-}|g" "$CONFIG"
  sed -i "s|__SMTP_PASSWORD__|${SMTP_PASSWORD:-}|g" "$CONFIG"
  sed -i "s|__ALERT_EMAIL_TO__|${ALERT_EMAIL_TO}|g" "$CONFIG"
else
  sed -i '/email_configs:/,/send_resolved: true/d' "$CONFIG"
  sed -i '/smtp_/d' "$CONFIG"
fi

exec /bin/alertmanager "$@"
