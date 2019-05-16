branch_name="$(git symbolic-ref HEAD 2>/dev/null)" ||
branch_name="(unnamed branch)"     # detached HEAD
branch_name=${branch_name##refs/heads/}

mvn install -Dproduction -DskipTests
scp perun-rpc/target/perun-rpc.war root@78.128.250.238:~/webapps/

# Write deployed branch name
ssh root@78.128.250.238 "echo $branch_name @ $(date) > ~/webapps/deployedBranch.txt"
echo "Deployed $branch_name branch"
