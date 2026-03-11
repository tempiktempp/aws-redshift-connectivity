redshift:
  connection-strategy: DATA_API
  credentials-strategy: ENVIRONMENT
  region: ${AWS_REGION:us-east-1}
  database: ${REDSHIFT_DATABASE:dev}
  cluster-identifier: ${REDSHIFT_CLUSTER_ID:your-cluster-id}
  db-user: ${REDSHIFT_DB_USER:your-db-user}
  # ── Proxy (remove if not needed) ──────────────────────────────
  proxy:
    enabled: true
    host: ${PROXY_HOST:proxy.yourcompany.com}
    port: ${PROXY_PORT:8080}
    username: ${PROXY_USERNAME:}   # leave blank if no auth needed
    password: ${PROXY_PASSWORD:}   # leave blank if no auth needed
