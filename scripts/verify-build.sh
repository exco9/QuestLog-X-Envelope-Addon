#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST_DIR="${ROOT_DIR}/dist"

rm -rf "${DIST_DIR}"
mkdir -p "${DIST_DIR}"

collect_release_jars() {
  local module="$1"
  local found=0

  shopt -s nullglob
  for jar in "${ROOT_DIR}/${module}/build/libs/"*.jar; do
    case "$(basename "${jar}")" in
      *-sources.jar|*-dev.jar|*-dev-shadow.jar)
        continue
        ;;
    esac

    cp "${jar}" "${DIST_DIR}/"
    found=$((found + 1))
  done
  shopt -u nullglob

  if [[ "${found}" -eq 0 ]]; then
    echo "No distributable jar found for ${module}." >&2
    exit 1
  fi
}

assert_entry() {
  local jar="$1"
  local entry="$2"
  if ! jar tf "${jar}" | grep -Fxq "${entry}"; then
    echo "Missing ${entry} in $(basename "${jar}")" >&2
    exit 1
  fi
}

assert_json_entry() {
  local jar="$1"
  local entry="$2"
  assert_entry "${jar}" "${entry}"
  unzip -p "${jar}" "${entry}" | python3 -m json.tool >/dev/null
}

assert_expanded_metadata() {
  local jar="$1"
  local entry="$2"
  if unzip -p "${jar}" "${entry}" | grep -Fq '${'; then
    echo "Unexpanded template variable in ${entry} inside $(basename "${jar}")" >&2
    exit 1
  fi
}

assert_common_payload() {
  local jar="$1"
  assert_entry "${jar}" 'io/github/exco9/questlogenvelope/QuestlogEnvelope.class'
  assert_json_entry "${jar}" 'questlog_envelope.mixins.json'
  assert_json_entry "${jar}" 'assets/questlog_envelope/lang/en_us.json'
  assert_json_entry "${jar}" 'assets/questlog_envelope/lang/fr_fr.json'
  assert_entry "${jar}" 'assets/questlog_envelope/textures/gui/magic_circle.png'
  assert_json_entry "${jar}" 'assets/questlog_envelope/font/signature.json'
  assert_entry "${jar}" 'assets/questlog_envelope/font/bitscript.ttf'
  assert_entry "${jar}" 'assets/questlog_envelope/textures/font/signature.png'
  assert_entry "${jar}" 'assets/questlog_envelope/font/bitscript-license.txt'
  assert_entry "${jar}" 'assets/questlog_envelope/font/bitscript_r2.png'
  assert_entry "${jar}" 'io/github/exco9/questlogenvelope/mail/SignatureActions.class'
  assert_entry "${jar}" 'io/github/exco9/questlogenvelope/network/SignLetterC2SP.class'
  assert_entry "${jar}" 'io/github/exco9/questlogenvelope/client/EmberTextCompat.class'
}

collect_release_jars fabric
collect_release_jars neoforge

fabric_checked=0
neoforge_checked=0

shopt -s nullglob
for jar in "${DIST_DIR}/"*.jar; do
  if jar tf "${jar}" | grep -Fxq 'fabric.mod.json'; then
    assert_common_payload "${jar}"
    assert_entry "${jar}" 'io/github/exco9/questlogenvelope/fabric/QuestlogEnvelopeFabric.class'
    assert_entry "${jar}" 'io/github/exco9/questlogenvelope/client/fabric/EmberTextCompatImpl.class'
    assert_json_entry "${jar}" 'fabric.mod.json'
    assert_expanded_metadata "${jar}" 'fabric.mod.json'
    if ! unzip -p "${jar}" fabric.mod.json | grep -Fq 'GPL-3.0-only'; then
      echo "Fabric metadata does not declare GPL-3.0-only in $(basename "${jar}")" >&2
      exit 1
    fi
    fabric_checked=$((fabric_checked + 1))
  fi

  if jar tf "${jar}" | grep -Fxq 'META-INF/neoforge.mods.toml'; then
    assert_common_payload "${jar}"
    assert_entry "${jar}" 'io/github/exco9/questlogenvelope/neoforge/QuestlogEnvelopeNeoForge.class'
    assert_entry "${jar}" 'io/github/exco9/questlogenvelope/client/neoforge/EmberTextCompatImpl.class'
    assert_expanded_metadata "${jar}" 'META-INF/neoforge.mods.toml'
    if ! unzip -p "${jar}" META-INF/neoforge.mods.toml | grep -Fq 'GPL-3.0-only'; then
      echo "NeoForge metadata does not declare GPL-3.0-only in $(basename "${jar}")" >&2
      exit 1
    fi
    neoforge_checked=$((neoforge_checked + 1))
  fi
done
shopt -u nullglob

if [[ "${fabric_checked}" -lt 1 ]]; then
  echo 'No Fabric distributable passed metadata checks.' >&2
  exit 1
fi

if [[ "${neoforge_checked}" -lt 1 ]]; then
  echo 'No NeoForge distributable passed metadata checks.' >&2
  exit 1
fi

echo "Verified ${fabric_checked} Fabric jar(s) and ${neoforge_checked} NeoForge jar(s)."
printf 'Release artifacts:\n'
find "${DIST_DIR}" -maxdepth 1 -type f -name '*.jar' -printf '  %f\n' | sort
