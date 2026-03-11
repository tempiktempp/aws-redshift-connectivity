aws redshift-data execute-statement ^
  --cluster-identifier your-cluster-id ^
  --database your-database ^
  --db-user your-db-user ^
  --sql "SELECT 1" ^
  --region us-east-1
