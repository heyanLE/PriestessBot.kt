#!/bin/bash

JAVA_HOME=/Users/heyanle/Library/Java/JavaVirtualMachines/jbrsdk_jcef-21.0.10/Contents/Home
PROJECT_DIR=/Users/heyanle/Desktop/project/astrbot.kt
CACHE=/Users/heyanle/.gradle/caches/modules-2/files-2.1
BUILD=$PROJECT_DIR/build/libs/astrbot.kt-1.0-SNAPSHOT.jar
CONFIG_PATH=${PRIESTESS_CONFIG_PATH:-$PROJECT_DIR/manual-test-logs/telegram-tools-smoke/config.json}
RUNTIME_DIR=${PRIESTESS_RUNTIME_DIR:-/private/tmp/priestess-smoke-runtime}
RUNTIME_JAR="$RUNTIME_DIR/astrbot.kt-$(date +%s).jar"

echo "Using config: $CONFIG_PATH"

add_all_jars() {
  local base="$1" version="$2"
  if [ -d "$base" ]; then
    for artifact_dir in "$base"/*; do
      [ -d "$artifact_dir" ] || continue
      local ver_dir="$artifact_dir/$version"
      if [ -d "$ver_dir" ]; then
        find "$ver_dir" -maxdepth 2 -name '*.jar' \
          ! -name '*-sources.jar' \
          ! -name '*-javadoc.jar' \
          ! -name '*android*' \
          ! -name '*test*' \
          2>/dev/null
      fi
    done
  fi
}

mkdir -p "$RUNTIME_DIR"
find "$RUNTIME_DIR" -type f -name 'astrbot.kt-*.jar' -mtime +1 -delete 2>/dev/null
cp "$BUILD" "$RUNTIME_JAR"

CP="$RUNTIME_JAR"

CP="$CP:$(add_all_jars "$CACHE/org.jetbrains.kotlin" "2.3.20" | tr '\n' ':')"
CP="$CP:$(add_all_jars "$CACHE/org.jetbrains.kotlinx" "1.9.0" | tr '\n' ':')"
CP="$CP:$(add_all_jars "$CACHE/org.jetbrains.kotlinx" "1.7.3" | tr '\n' ':')"
CP="$CP:$(add_all_jars "$CACHE/org.jetbrains.kotlinx" "0.5.4" | tr '\n' ':')"
CP="$CP:$(add_all_jars "$CACHE/io.ktor" "3.0.3" | tr '\n' ':')"
CP="$CP:$(add_all_jars "$CACHE/io.insert-koin" "4.0.2" | tr '\n' ':')"
CP="$CP:$(add_all_jars "$CACHE/io.github.oshai" "7.0.13" | tr '\n' ':')"
CP="$CP:$(add_all_jars "$CACHE/ch.qos.logback" "1.5.18" | tr '\n' ':')"
CP="$CP:$(add_all_jars "$CACHE/org.slf4j" "2.0.17" | tr '\n' ':')"
CP="$CP:$(add_all_jars "$CACHE/org.jetbrains.exposed" "0.51.1" | tr '\n' ':')"
CP="$CP:$(add_all_jars "$CACHE/org.xerial" "3.46.1.3" | tr '\n' ':')"
CP="$CP:$(add_all_jars "$CACHE/org.snakeyaml" "2.7" | tr '\n' ':')"
CP="$CP:$(add_all_jars "$CACHE/io.netty" "4.1.116.Final" | tr '\n' ':')"

CP=$(echo "$CP" | sed 's/::*/:/g' | sed 's/:$//' | sed 's/^://')

jar_count=$(echo "$CP" | tr ':' '\n' | grep -c '\.jar$')
echo "Total jars: $jar_count"

export JAVA_HOME
export PRIESTESS_CONFIG_PATH="$CONFIG_PATH"
JAVA_BIN="${JAVA_HOME}/bin/java"
if [ ! -x "$JAVA_BIN" ]; then
  JAVA_BIN="$(command -v java)"
fi

exec "$JAVA_BIN" -cp "$CP" com.heyanle.priestess.bot.PriestessBotKt
