NAME=$1
docker stop $NAME || true
docker rm $NAME || true