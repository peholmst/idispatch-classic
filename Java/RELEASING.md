# Versions and releases

The repository holds two products that are released separately, each with its own version:

| | Server | Runboard |
|---|---|---|
| Directory | `server/` (the application in `server/app` and the ASPSMS client in `server/aspsms-integration`, which share its version) | `runboard/` |
| Tag | `idispatch-server/v<version>` | `idispatch-runboard/v<version>` |
| Distributed as | Its sources at the tag. An organisation checks out the tag and builds and runs it with Docker Compose (`docs/deployment.md`). | `iDispatchRunboard-<version>.zip`, attached to the GitHub Release, unzipped on the runboard computer. |

`pom.xml` in this directory only groups the two for the IDE and CI; it is not a parent, and has no version of its own.

Versions are `MAJOR.MINOR.PATCH`. Between releases a product carries the next minor version as a snapshot, such as
`3.1-SNAPSHOT`. The server shows its version in the drawer; the runboard's zip has it in its name and in `README.txt`.

The server and the runboard talk over the `/ws/runboard` WebSocket. A release that changes that protocol says in its
notes which versions of the other product it works with.

## Releasing

1. On an up-to-date `main` with a clean working tree, run

   ```shell
   ./release.sh <server|runboard> <version> <next development version>
   ./release.sh runboard 3.0.0 3.1-SNAPSHOT
   ```

   It commits the release version (if the POMs don't already have it), tags that commit, and commits the next
   development version. It pushes nothing.

2. Push the commits and the tag together, as the script tells you:

   ```shell
   git push --atomic origin main idispatch-runboard/v3.0.0
   ```

3. The tag starts the `Release` workflow (`.github/workflows/release.yml`). It checks that the tag matches the
   version in the POM, builds and tests the product, and creates a draft GitHub Release. The runboard's has the zip
   attached; the server's has upgrade instructions, since it is distributed as source.

4. Fill in the draft: what changed for this product (the generated list covers every pull request in the repository
   since the product's previous release) and which versions of the other product it works with. Then publish it.

The runboard's release is marked as the latest release on GitHub, so that the repository's front page links to the
zip; the server's is not.

If the workflow fails, fix the problem on `main`, delete the tag (`git push origin :refs/tags/<tag>` and
`git tag -d <tag>`) and release again from the new commit.
