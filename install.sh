#!/bin/bash

set -e

cd ./out/production/mijavco/
jar cfve mijavco.jar compiler.Mijavco $(find . -name '*.class')
mv mijavco.jar ../../../
cd ../../../

cat \
  <(echo '#!/bin/sh')\
  <(echo 'exec java -jar $0 "$@"')\
  <(echo 'exit 0')\
  mijavco.jar > mijavco

chmod +x mijavco
sudo mv mijavco /usr/bin/

