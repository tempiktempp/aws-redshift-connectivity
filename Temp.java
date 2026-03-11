# ─────────────────────────────────────────────────────────────────
# local-env.sh — Local development environment variables
#
# Usage:
#   First time or full setup:  source local-env.sh
#   Rotate expired credentials: source local-env.sh rotate
#
# IMPORTANT:
#   Add local-env.sh to .gitignore immediately.
#   Never commit this file — it contains real credentials.
# ─────────────────────────────────────────────────────────────────

# ── Redshift Config (stable — rarely changes) ─────────────────────
export AWS_REGION=us-east-1
export REDSHIFT_DATABASE=your-database-name
export REDSHIFT_CLUSTER_ID=your-cluster-identifier
export REDSHIFT_DB_USER=your-db-user

# ── AWS Credentials ───────────────────────────────────────────────
# These expire every hour if using temporary session tokens.
# Run: source local-env.sh rotate
# to update just these three without re-setting everything above.

if [ "$1" = "rotate" ]; then
    echo "[env] Rotating AWS credentials only..."

    export AWS_ACCESS_KEY_ID=your-new-access-key
    export AWS_SECRET_ACCESS_KEY=your-new-secret-key
    export AWS_SESSION_TOKEN=your-new-session-token

    echo "[env] Credentials rotated."
    echo "[env] AWS_ACCESS_KEY_ID  = $AWS_ACCESS_KEY_ID"
    echo "[env] Session token set  = ${AWS_SESSION_TOKEN:0:20}..."
else
    echo "[env] Loading all environment variables..."

    export AWS_ACCESS_KEY_ID=your-access-key
    export AWS_SECRET_ACCESS_KEY=your-secret-key
    export AWS_SESSION_TOKEN=your-session-token

    echo "[env] All variables loaded."
    echo "[env] AWS_REGION         = $AWS_REGION"
    echo "[env] REDSHIFT_DATABASE  = $REDSHIFT_DATABASE"
    echo "[env] REDSHIFT_CLUSTER_ID= $REDSHIFT_CLUSTER_ID"
    echo "[env] REDSHIFT_DB_USER   = $REDSHIFT_DB_USER"
    echo "[env] AWS_ACCESS_KEY_ID  = $AWS_ACCESS_KEY_ID"
    echo "[env] Session token set  = ${AWS_SESSION_TOKEN:0:20}..."
fi

echo "[env] Done."
```

---

### Add to .gitignore immediately

Open or create `.gitignore` in `edp-parent` root and add:
```
# Local environment variables — contains real credentials
local-env.sh

# IntelliJ
.idea/
*.iml

# Maven
target/
