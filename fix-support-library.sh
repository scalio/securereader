#!/bin/bash

# we must use the same support lib jar in all the dependencies
good_jar="app/libs/android-support-v4.jar"

# all these libs depend on android-support-v4.jar
#mapfile <<END # requires newer bash than on MacOS X
#external/ActionBarSherlock/actionbarsherlock
#external/CacheWord/cachewordlib
#END

MAPFILE[0]='external/HoloEverywhere/ActionBarSherlock/actionbarsherlock'
MAPFILE[1]='external/CacheWord/cachewordlib'
MAPFILE[2]='external/OnionKit/libnetcipher'


for project in "${MAPFILE[@]}"; do
    project=${project%$'\n'} # remove trailing newline
    echo "updating $good_jar in $project"
    cp -f $good_jar $project/libs
done

rm -f external/bho/TbChat/libs/guava-r09.jar
rm -f external/CacheWord/cachewordlib/libs/guava-r09.jar
cp external/securereaderlibrary/libs/guava-11.0.2.jar external/bho/TbChat/libs/
cp external/securereaderlibrary/libs/guava-11.0.2.jar external/CacheWord/cachewordlib/libs/

cp external/CacheWord/cachewordlib/libs/sqlcipher.jar external/securereaderlibrary/libs/sqlcipher.jar 
cp external/CacheWord/cachewordlib/libs/iocipher.jar external/securereaderlibrary/libs/iocipher.jar 

