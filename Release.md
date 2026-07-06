# JarHC Gradle Plugin Release Checklist

This checklist releases version `3.1.0`. Replace the version number as needed.

Note: direct pushes to `main` are blocked by a GitHub ruleset (pull request +
Copilot review required). Every commit, including the version bumps below, goes
through a branch and a pull request.

## 1. Preparation

Start from a clean, up-to-date `main`:

```shell
git checkout main
git pull
git status   # working tree must be clean
```

Verify the release credentials are available in `~/.gradle/gradle.properties`
(signing key and `gradle.publish.key` / `gradle.publish.secret`), so the publish
step later does not fail after tagging.

Check if all dependencies are up to date:

```shell
./gradlew dependencyUpdates
```

Review the latest SonarQube report:

https://sonarcloud.io/summary/new_code?id=smarkwal_jarhc-gradle-plugin

Confirm the versions in the code and docs are consistent with this release:

- `DEFAULT_JARHC_VERSION` in `JarhcGradlePlugin.java` (the default JarHC version resolved by the `jarhc` configuration).
- `MINIMUM_GRADLE_VERSION` in `JarhcGradlePlugin.java` and the "requires Gradle
  X.Y" line in `README.md`.

## 2. Set the release version (via pull request)

Create a release branch:

```shell
git checkout -b release/3.1.0
```

Update version in `gradle.properties`:

```properties
version = 3.1.0
```

Update the version number in the example code in `README.md`:

```kotlin
plugins {
    id("org.jarhc") version "3.1.0"
}
```

Build and test everything:

```shell
./gradlew clean build
```

Commit, push, and open a pull request. Use the branch name as the PR title and
write a short description of the release in the body:

```shell
git commit -am "Release: Change version from 3.1.0-SNAPSHOT to 3.1.0."
git push -u origin release/3.1.0
gh pr create --title "release/3.1.0" --body "<summary of the release>"
```

Review the PR yourself. Once you have reviewed it and CI and Copilot have passed,
merge it:

```shell
gh pr merge --squash
```

## 3. Tag the release

Return to `main` and pull the merged version bump:

```shell
git checkout main
git pull
```

Create the release tag on the merge commit:

```shell
git tag v3.1.0
```

## 4. Publish to the Gradle Plugin Portal

First validate the publication without releasing it (checks signing, credentials,
and the publication metadata):

```shell
./gradlew publishPlugins --validate-only
```

If validation passes, publish for real:

```shell
./gradlew publishPlugins
```

Wait for the publication to appear (indexing can take a few minutes):

https://plugins.gradle.org/plugin/org.jarhc

Push the tag to GitHub:

```shell
git push origin v3.1.0
```

## 5. Create the GitHub release (draft)

Write the release notes by hand in the same terse style as previous releases
(short `* Change:` / `* Feature:` bullets, user-facing changes only).

Review the previous notes for the style, and the commits for the content:

```shell
gh release view v1.2.0 --json body --jq .body
git log --oneline v1.2.0..v3.1.0
```

Write the notes to a file, then create the draft release from it:

```shell
gh release create v3.1.0 --draft --title "v3.1.0" --notes-file release-notes.md
```

Review and edit the draft in the UI, and leave it as a draft until the smoke test
in step 6 passes:

https://github.com/smarkwal/jarhc-gradle-plugin/releases

## 6. Smoke test the published plugin, then publish the release

In a throwaway project, apply the plugin from the portal and run the task to
confirm the artifact resolves and works:

```kotlin
plugins {
    id("org.jarhc") version "3.1.0"
}
```

```shell
./gradlew jarhcReport
```

Check the task output for unexpected warnings or errors, not just that the build
succeeds.

Once the smoke test passes, publish the draft release:

```shell
gh release edit v3.1.0 --draft=false
```

## 7. Set the next snapshot version (via pull request)

```shell
git checkout main
git pull
git checkout -b project/next-snapshot
```

Change version to the next snapshot version in `gradle.properties`:

```properties
version = 3.1.1-SNAPSHOT
```

Commit, push, and open a pull request:

```shell
git commit -am "Project: Change version from 3.1.0 to 3.1.1-SNAPSHOT."
git push -u origin project/next-snapshot
gh pr create --title "project/next-snapshot" --body "<summary of the change>"
```

Review the PR yourself, then merge once CI and Copilot have passed:

```shell
gh pr merge --squash
```

## 8. Update the JarHC project

Consider updating the JarHC Gradle plugin version in the JarHC project to the new
release version:

https://github.com/smarkwal/jarhc/blob/main/gradle/libs.versions.toml#L51

Also update any plugin version references in the JarHC documentation site
(`website/docs/gradle-plugin.md`).
