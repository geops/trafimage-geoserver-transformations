#!/bin/bash
# echo the git commit hash which gives the best description
# of the current version of the source

set -eu

GIT=$(which git)
GIT_COMMIT_HASH="Not build from a git repository."
if [ -d ".git" ]; then
    if [ -x "$GIT" ]; then
        GIT_COMMIT_HASH=$($GIT rev-parse HEAD)

        # check for local changes
        if ! $GIT diff-index --quiet HEAD --; then
            GIT_COMMIT_HASH="$GIT_COMMIT_HASH-dirty"
        fi
    fi
fi
echo "$GIT_COMMIT_HASH"
