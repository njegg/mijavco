#!/bin/bash

cat \
  <(echo '#!/bin/sh')\
  <(echo 'exec java -jar $0 "$@"')\
  <(echo 'exit 0')\
  mijavco.jar > mijavco

chmod +x mijavco

