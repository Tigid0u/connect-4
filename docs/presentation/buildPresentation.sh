#!/usr/bin/env sh

## This script is inspired of https://github.com/heig-vd-dai-course/heig-vd-dai-course/raw/refs/heads/main/build-all-presentations.sh

## Variables
MARP_DOCKER_IMAGE="marpteam/marp-cli:v4.1.1"
WORKDIR=$(pwd)
MARP_CMD="docker run --rm --entrypoint=\"marp-cli.js\" --volume=\"$WORKDIR\":/home/marp/app $MARP_DOCKER_IMAGE"

## Convert presentation to PDF
echo "Converting presentation to PDF"
eval "$MARP_CMD --pdf ./docs/presentation/PRESENTATION.md"

## Convert presentation to HTML
echo "Converting presentation to HTML"
eval "$MARP_CMD --pdf ./docs/presentation/PRESENTATION.md"