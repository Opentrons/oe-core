#! /bin/bash

case "$-" in
*i*)	echo Building ot3 image ;;
*)	echo This script uses docker run -i and will fail because this shell is not interactive ;;
esac

branch=  
while getopts ":b:" opt; do
  case $opt in
    b)
      branch=$OPTARG >&2
      echo $OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      exit 1
      ;;
  esac
done
shift $((OPTIND - 1))


docker build --build-arg "host_uid=1000"   --build-arg "host_gid=1000" --tag "ot3-image:latest" .
docker run -it --mount type=bind,src=$PWD,dst=/home/ot3/oe-core,consistency=delegated ot3-image:latest
