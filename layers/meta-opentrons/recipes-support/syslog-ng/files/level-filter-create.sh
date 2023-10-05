#!/usr/bin/env sh

LOG_LEVEL_DIR=@STATEDIR@/lib/syslog-ng
LOG_LEVEL_FILE=${LOG_LEVEL_DIR}/min-level

if [ ! -f ${LOG_LEVEL_FILE} ]
then
    mkdir -p ${LOG_LEVEL_DIR}
    echo "debug" > ${LOG_LEVEL_FILE}
fi

level=$(cat ${LOG_LEVEL_FILE})

# The actual filter statement. This sets the minimum
# log level to what was in the level file
echo "level(${level}..emerg);"
