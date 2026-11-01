#!/usr/bin/env bash
# Releases the server or the runboard. See RELEASING.md.
#
#   ./release.sh <server|runboard> <version> <next development version>
#   ./release.sh runboard 3.0.0 3.1-SNAPSHOT
#
# On an up-to-date main with a clean working tree: sets the component's version to <version> and commits it (unless
# it already is that version), tags that commit idispatch-<component>/v<version>, then sets the version to the next
# development version and commits that. Nothing is pushed; the script prints the command that pushes, which starts
# the release workflow.
set -euo pipefail

if [ $# -ne 3 ]; then
    echo "Usage: $0 <server|runboard> <version> <next development version>" >&2
    exit 2
fi
component=$1
version=$2
next=$3

case "$component" in
    server) name="iDispatch Server" ;;
    runboard) name="iDispatch Runboard" ;;
    *) echo "Unknown component '$component': use server or runboard" >&2; exit 2 ;;
esac
if ! [[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "The version must be X.Y.Z, not '$version'" >&2
    exit 2
fi
if [[ "$next" != *-SNAPSHOT ]]; then
    echo "The next development version must end in -SNAPSHOT, not '$next'" >&2
    exit 2
fi

cd "$(dirname "$0")"
dir=$component
tag="idispatch-$component/v$version"

if [ "$(git rev-parse --abbrev-ref HEAD)" != main ]; then
    echo "Release from main" >&2
    exit 1
fi
if [ -n "$(git status --porcelain)" ]; then
    echo "The working tree has changes; commit or stash them first" >&2
    exit 1
fi
git fetch --quiet --tags origin main
if [ "$(git rev-parse HEAD)" != "$(git rev-parse origin/main)" ]; then
    echo "main is not the same as origin/main; pull or push first" >&2
    exit 1
fi
if git rev-parse --quiet --verify "refs/tags/$tag" > /dev/null; then
    echo "The tag $tag already exists" >&2
    exit 1
fi

project_version() {
    mvn -B -q -f "$dir/pom.xml" help:evaluate -Dexpression=project.version -DforceStdout
}

set_version() {
    # From the component's top POM, which also updates the modules under it
    mvn -B -q -f "$dir/pom.xml" versions:set -DnewVersion="$1" -DgenerateBackupPoms=false
    git add -- "$dir"
    git commit --quiet -m "$2"
}

if [ "$(project_version)" != "$version" ]; then
    set_version "$version" "Release $name $version"
fi
git tag -a "$tag" -m "$name $version"
set_version "$next" "Start $name $next"

echo
echo "Tagged $tag and moved $dir to $next. Check with 'git log --oneline -3', then push:"
echo
echo "    git push --atomic origin main $tag"
echo
echo "The release workflow then creates a draft GitHub Release to fill in and publish."
