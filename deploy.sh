branch_name="$(git symbolic-ref HEAD 2>/dev/null)" ||
branch_name="(unnamed branch)"     # detached HEAD
branch_name=${branch_name##refs/heads/}

mvn clean install -DskipTests
scp perun-rpc/target/perun-rpc.war root@147.251.124.100:/var/lib/tomcat8/webapps

# Write deployed branch name
ssh root@147.251.124.100 "echo $branch_name @ $(date) > /var/lib/tomcat8/webapps/deployedBranch.txt"
echo "Deployed $branch_name branch"
