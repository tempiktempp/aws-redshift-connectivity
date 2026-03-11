aws redshift-data list-statements --region us-east-1 --debug 2>&1 | findstr /i "proxy\|connect\|endpoint\|host"
curl -v --proxy http://john.doe:password@proxyhost:8080 https://redshift-data.us-east-1.amazonaws.com 2>&1
