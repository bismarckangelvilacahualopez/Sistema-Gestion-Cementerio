#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build
javac --release 17 -encoding UTF-8 -d build src/cementerio/Main.java pruebas/PruebaAutomatica.java
java -cp build PruebaAutomatica
