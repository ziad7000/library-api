#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Maven Start Up Batch script
#
# Required ENV Vars:
# ------------------
#   JAVA_HOME - location of a JDK home dir
#
# Optional ENV Vars
# -----------------
#   M2_HOME - location of maven2's installed home dir
#   MAVEN_OPTS - parameters to passed to the Java VM when running Maven
#     e.g. to debug Maven itself, use
#       set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
#   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
# ----------------------------------------------------------------------------

if [ -z "$MAVEN_SKIP_RC" ]; then
  if [ -f /etc/mavenrc ]; then
    . /etc/mavenrc
  fi
  if [ -f "$HOME/.mavenrc" ]; then
    . "$HOME/.mavenrc"
  fi
fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
mingw=false
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  MINGW*) mingw=true;;
  Darwin*) darwin=true ;;
esac

## resolve links - $0 may be a link to maven's home
PRG="$0"

# need this for relative symlinks
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG="`dirname "$PRG"`/$link"
  fi
done

saveddir=`pwd`

BASE_DIR=`dirname "$PRG"`

# Reset CDPATH if set
[ -n "$CDPATH" ] && unset CDPATH

# Change to the base directory of the project
cd "$BASE_DIR"

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$M2_HOME" ] &&
    M2_HOME=`cygpath --unix "$M2_HOME"`
  [ -n "$JAVACMD" ] &&
    JAVACMD=`cygpath --unix "$JAVACMD"`
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$BASE_DIR" ] &&
    BASE_DIR=`cygpath --unix "$BASE_DIR"`
fi

if [ -z "$JAVACMD" ] ; then
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD="`which java 2>/dev/null`"
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly." >&2
  echo "  We cannot execute $JAVACMD" >&2
  exit 1
fi

WRAPPER_JAR="$BASE_DIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

# Determine Maven wrapper properties
WRAPPER_PROPERTIES="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

# If wrapper jar does not exist, download distribution or compile
if [ ! -f "$WRAPPER_JAR" ]; then
  mkdir -p "$BASE_DIR/.mvn/wrapper"
  # Fetch wrapper distribution url
  distUrl=$(grep -E "^distributionUrl=" "$WRAPPER_PROPERTIES" | cut -d= -f2- | tr -d '\r')
  mvnVersion=$(echo "$distUrl" | sed -E 's/.*apache-maven-([0-9.]+)-bin.zip/\1/')
  MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-$mvnVersion"
  if [ ! -d "$MAVEN_HOME" ]; then
    echo "Downloading Maven $mvnVersion..."
    mkdir -p "$MAVEN_HOME"
    tmpZip="$MAVEN_HOME/maven.zip"
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "$distUrl" -o "$tmpZip"
    elif command -v wget >/dev/null 2>&1; then
      wget -q "$distUrl" -O "$tmpZip"
    else
      echo "Error: curl or wget is required to download Maven wrapper." >&2
      exit 1
    fi
    unzip -qo "$tmpZip" -d "$MAVEN_HOME"
    rm -f "$tmpZip"
    extractedDir=$(find "$MAVEN_HOME" -mindepth 1 -maxdepth 1 -type d | head -n 1)
    if [ -n "$extractedDir" ] && [ "$extractedDir" != "$MAVEN_HOME/bin" ]; then
      mv "$extractedDir"/* "$MAVEN_HOME"/ 2>/dev/null || true
      mv "$extractedDir"/.* "$MAVEN_HOME"/ 2>/dev/null || true
      rmdir "$extractedDir" 2>/dev/null || true
    fi
  fi
  exec "$MAVEN_HOME/bin/mvn" "$@"
fi

exec "$JAVACMD" \
  $MAVEN_OPTS \
  -classpath "$WRAPPER_JAR" \
  "-Dmaven.home=$M2_HOME" \
  "-Dmaven.multiModuleProjectDirectory=$BASE_DIR" \
  $WRAPPER_LAUNCHER "$@"
