#!/usr/bin/env bash
#
# Validate res/raw/watchface.xml against the official Watch Face Format XSD.
#
# Google publishes the schema in google/watchface but ships no pre-built
# validator jar, so this clones the spec once into .wff-spec/ (gitignored)
# and validates with Xerces, which the schema needs for its XSD 1.1
# assertions.
#
#   ./tools/validate.sh            # validate the face at format version 1
#   ./tools/validate.sh 4          # validate against a later format version
#
set -euo pipefail

VERSION="${1:-1}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SPEC="$ROOT/.wff-spec"
WFF="$SPEC/third_party/wff/specification"
FACE="$ROOT/app/src/main/res/raw/watchface.xml"

if [[ ! -d "$SPEC" ]]; then
  echo "Fetching Watch Face Format specification..."
  git clone --depth 1 --quiet https://github.com/google/watchface.git "$SPEC"
fi

SCHEMA="$WFF/documents/$VERSION/watchface.xsd"
if [[ ! -f "$SCHEMA" ]]; then
  echo "No schema for format version $VERSION." >&2
  echo "Available: $(ls "$WFF/documents" | tr '\n' ' ')" >&2
  exit 2
fi

LIBS="$WFF/validator/libs"
CP="$LIBS/xercesImpl.jar:$LIBS/xml-apis.jar"
CP="$CP:$LIBS/org.eclipse.wst.xml.xpath2.processor_1.2.1.jar:$LIBS/cupv10k-runtime.jar"

OUT="$ROOT/.wff-spec/.classes"
mkdir -p "$OUT"
javac -nowarn -cp "$CP" -d "$OUT" "$ROOT/tools/WffValidate.java"

echo "watchface.xml against WFF v$VERSION:"
java -cp "$CP:$OUT" WffValidate "$SCHEMA" "$FACE"
