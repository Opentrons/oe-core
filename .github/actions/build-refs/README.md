# Determining References To Build

This github action is used to determine which git references of the repos that go into an openembedded build to use, based on some of them being specified and others of them being left free. This allows us to start builds where we only specify the git refs that we care about, and leave the rest to be automatically decided following these rules:

- When a build is started, identify the "authoritative ref", which shall be the first of the following repos in order that is in `S: opentrons, oe-core`. The authoritative ref is `A`.
  - For each repo in `S`, use the specified source revision
  - For each repo `Fi` in `F`
    - if `A` is the default branch of its repo (i.e. `edge` for monorepo, `main` for oe-core), use the default branch of `Fi`
    - if `A` is a branch that is not the default branch of its repo
      - and a branch of the same name exists in `Fi`, use that matching branch
      - and a branch of the same name does not exist in `Fi`, use the default branch of `Fi`
    - if `A` is a tag
      - for each repo `Fi` whose ref is not specified, use that tag on `Fi`
      - fail the build if that tag does not exist on `Fi`
    - if `A` is a coordinated release tag and `Fi` is `ot3-firmware`
      - external builds: map stack semver `v9.1.0-alpha.7` to firmware coordination tag `ex9.1.0-alpha.7`
      - internal builds: use the same `ot3@8.5.0-alpha.1` tag on ot3-firmware
      - checkout the firmware coordination tag so cmake sees the integer version tag on that commit
      - do not place stack-style semver `v*` tags on ot3-firmware (they collide with `git describe --match=v*`)

### ot3-firmware dual tagging at release

Use three different tag shapes. Do not conflate them:

| Tag shape | Example | Repo | Role |
|---|---|---|---|
| Stack external semver | `v9.1.0-alpha.7` | opentrons, oe-core | Coordinated release marker |
| Firmware coordination | `ex9.1.0-alpha.7` or `ot3@8.5.0-alpha.1` | ot3-firmware | Which commit CI checks out |
| Firmware integer version | `v70` | ot3-firmware | Firmware version integer for cmake (`v` + digits only, no dots) |

The integer version tag is **`vN`** (for example `v70`, not `v9.1.0`). That is unrelated to stack semver tags that happen to start with `v`.

**Integer `vN` rules on ot3-firmware:**

- Every commit that carries a firmware coordination tag (`ex*` or `ot3@*`) must also have **exactly one** integer version tag (`vN`) pointing at the **same commit**.
- **You do not need a new `vN` for every stack release.** Bump the integer only when the firmware version actually increases (for example `v70` to `v71`).
- If a coordination release reuses the same firmware commit, **reuse the existing `vN` already on that commit**. Add the new `ex*` or `ot3@*` tag only; do not mint another integer tag.
- Example: `ex9.1.0-alpha.7` and `ex9.1.0-alpha.8` on the same commit both use the same co-located `v70`.

CI checks out the coordination tag. cmake reads the integer `vN` on that commit via `git describe --match=v*`.

**External** (stack tag `v9.1.0-alpha.7`, firmware checkout `ex9.1.0-alpha.7`, new firmware version):

```bash
# ot3-firmware only (first time this commit is released as v70):
git tag -a v70 -m "Flex firmware v70"
git tag -a ex9.1.0-alpha.7 -m "Coordinated release marker"
git push origin v70 ex9.1.0-alpha.7

# opentrons / oe-core (unchanged):
git tag -a v9.1.0-alpha.7 -m "Coordinated release marker"
git push origin v9.1.0-alpha.7
```

**External, same firmware commit, new stack alpha** (reuse `v70`, add coordination tag only):

```bash
# ot3-firmware: v70 already points at this commit
git tag -a ex9.1.0-alpha.8 -m "Coordinated release marker"
git push origin ex9.1.0-alpha.8
```

**Internal** (stack tag `ot3@8.5.0-alpha.1`, same coordination tag on firmware):

```bash
# ot3-firmware (first time this commit is released as v70):
git tag -a v70 -m "Flex firmware v70"
git tag -a ot3@8.5.0-alpha.1 -m "Coordinated release marker"
git push origin v70 ot3@8.5.0-alpha.1

# opentrons / oe-core (unchanged):
git tag -a ot3@8.5.0-alpha.1 -m "Coordinated release marker"
git push origin ot3@8.5.0-alpha.1
```

build-refs resolves monorepo and oe-core to the stack coordination tag. ot3-firmware uses `ex*` for external releases and `ot3@*` for internal releases.

## Developing

This github action is written in typescript. Github Actions doesn't support typescript, it supports javascript. So the action is transpiled, which means there's a whole huge project setup to handle it.

There's a package.json here which means you can (and should) `npm install` in this repo to get it set up. Use node 20.

After that, `npm run build` will transpile the code to js in `dist/`. This needs to be checked in.

- `npm run lint` lints
- `npm run format` formats
- `npm run test` runs tests with jest

If you forget to build and check in the result, github actions will do it for you.
