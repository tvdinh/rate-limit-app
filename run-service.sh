#!/bin/bash
#Usage: ./run-service.sh <time-duration> <volume>
#Where <time-duration> is the time duration to apply rate limit, and <volume> is the total number of requests allowed within the time duration.
TIME_DURATION=$1
VOLUME=$2
if [[ -z $TIME_DURATION ]]; then
    TIME_DURATION=3600
fi
if [[ -z $VOLUME ]]; then
    VOLUME=100
fi

java -Dapp.throttle.ratelimit.time-duration=$TIME_DURATION -Dapp.throttle.ratelimit.volume=$VOLUME -jar exec/airtasker-0.0.1-SNAPSHOT.jar 