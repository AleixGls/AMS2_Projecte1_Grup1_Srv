#!/bin/bash
set -e

cd "$(dirname "$0")"

mainClass=${1:-grup1.server.Main}

# Compilar proyecto y generar classpath
mvn compile dependency:build-classpath \
    -Dmdep.outputFile=target/classpath.txt

# Obtener dependencias Maven
dependencyPath=$(cat target/classpath.txt)

# Ejecutar servidor
exec java \
    -cp "target/classes:$dependencyPath" \
    "$mainClass"