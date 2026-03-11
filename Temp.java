@echo off
REM ─────────────────────────────────────────────────────────────────
REM local-env.cmd — Stable local development config
REM
REM Usage:
REM   local-env.cmd
REM
REM When AWS credentials expire, run these manually in terminal:
REM   set AWS_ACCESS_KEY_ID=new-key
REM   set AWS_SECRET_ACCESS_KEY=new-secret
REM   set AWS_SESSION_TOKEN=new-token
REM
REM IMPORTANT: Add local-env.cmd to .gitignore — never commit this.
REM ─────────────────────────────────────────────────────────────────

set AWS_REGION=us-east-1
set REDSHIFT_DATABASE=your-database-name
set REDSHIFT_CLUSTER_ID=your-cluster-identifier
set REDSHIFT_DB_USER=your-db-user

echo [env] Stable config loaded.
echo [env] AWS_REGION          = %AWS_REGION%
echo [env] REDSHIFT_DATABASE   = %REDSHIFT_DATABASE%
echo [env] REDSHIFT_CLUSTER_ID = %REDSHIFT_CLUSTER_ID%
echo [env] REDSHIFT_DB_USER    = %REDSHIFT_DB_USER%
```

---

### Update `.gitignore`
```
local-env.cmd
