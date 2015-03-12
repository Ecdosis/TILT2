#!/bin/bash
TILT_DIR=`ls -d tilt_2*/ | sed -e "s:/::"`
cd $TILT_DIR
find . -type f ! -regex '.*.hg.*' ! -regex '.*?debian-binary.*' \
! -regex '.*?DEBIAN.*' -printf '%P ' | xargs md5sum > DEBIAN/md5sums
cd ..
if [ -e dist/TILT2.jar ]; then
  cp dist/TILT2.jar "$TILT_DIR/usr/local/bin/tilt/"
  chmod +x "$TILT_DIR/usr/local/bin/tilt/TILT2.jar"
fi
rsync -az ./lib/ "./$TILT_DIR/usr/local/bin/tilt/lib"
rsync -az ./static/ "./$TILT_DIR/usr/local/bin/tilt/static"
dpkg-deb --build "$TILT_DIR"
