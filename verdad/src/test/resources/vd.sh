#!/bin/sh

opt=$1
if [ "$opt" == "--version" ]
then
   cat <<EOF
\$Id: vd b53c509 2013-11-06 09:29:45 +0000 joe@example.com $
\$Id: Verdad.pm 0ca875f 2013-11-06 09:13:06 +0000 joe@example.com $
\$Id: Schema.pm fc09297 2013-11-06 03:52:13 +0000 joe@example.com $
EOF
   exit 0
fi

cd `dirname $0`
cat vd.json
