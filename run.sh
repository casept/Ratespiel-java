#!/bin/sh
set -eu pipefail
cd src/ || echo "Couldn't find src/ !" && exit
javac "Main.java Game.java"
java Main
