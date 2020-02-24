#! /bin/sh -

# Hook created by Captain Hook at ${formattedCreatedAt}.

hook_name=`basename "$0"`

debug() {
  if [ "$CAPTAIN_HOOK_DEBUG" = "true" ] || [ "$CAPTAIN_HOOK_DEBUG" = "1" ]; then
    echo "captain-hook:debug $1"
  fi
}

debug "Hook $hook_name started."

if [ "$CAPTAIN_HOOK_SKIP" = "true" ] || [ "$CAPTAIN_HOOK_SKIP" = "1" ]; then
  debug "CAPTAIN_HOOK_SKIP is set to $CAPTAIN_HOOK_SKIP: skipping hook."
  exit 0
fi

echo "$hook_name > ${hookScript}"
${hookScript}
