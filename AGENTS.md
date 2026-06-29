# AGENTS.md

Conventions for working in the JarHC Gradle Plugin repository. Keep this file tight:
short bullet lists of facts, minimal prose. Preserve this style when editing it.

## Workflow

- Pushes to `main` are blocked by GitHub.
- All changes go through a branch and a pull request.
- GitHub Copilot reviews every pull request.

## Project layout

- Multi-project build: root + `jarhc-gradle-plugin/` subproject (plugin code).
- Source sets: `test` (unit tests, Mockito), `functionalTest` (TestKit).
- Add the Apache 2.0 license header to new source files.

## Java and Gradle versions

- Plugin bytecode: Java 11.
- Tests run on: Java 17 (Gradle 9 TestKit requires it).
- Gradle wrapper: 9.6.1 (requires Java 17 to run).
- Minimum supported Gradle: 8.8 (`MINIMUM_GRADLE_VERSION` in `JarhcGradlePlugin`).

## Compatibility (enforced by the functional tests)

- Keep the plugin compilable to Java 11 bytecode.
- Using a Gradle API newer than the minimum supported version raises it; flag the trade-off and let the user decide.
- Keep the `jarhcReport` task configuration-cache compatible (e.g., no access to the `Project` object at execution time).
- The functional tests run on `{ current, 8.8 }` with `--configuration-cache`; keep both settings.

## Dependencies

- Versions live in the version catalog: `gradle/libs.versions.toml`.
- Check for updates: `./gradlew dependencyUpdates`.
- After changing any version, refresh the lockfiles: `./gradlew updateGradleLockfiles --write-locks`.
- Dependabot proposes updates monthly.

## GitHub Actions

- Pin every action by full commit SHA with a `# vX.Y.Z` comment.

## Building

- `./gradlew build` — compile, unit tests, functional tests, plugin validation.
- Requires Java 17 to run Gradle.
- `:sonar` runs automatically on CI; run it locally (with a Sonar token in your Gradle settings) only for a reason such as a Sonar plugin update, a Sonar config change, or checking Sonar compatibility with a new Java or Gradle version.

## Categories

Shared terms, used both as commit message prefixes and as branch name categories:

- `project` — project setup: dependency and tooling upgrades, version changes, build configuration
- `feature` — add a new feature
- `bugfix` — fix a bug
- `code` — refactor existing code
- `docs` — update documentation
- `ci` — continuous integration, such as GitHub Actions workflows
- `hotfix` — urgent fix

## Commit messages

- Always start the subject with a category prefix: a term from the list above, capitalized, followed by a colon. Example: "Project: Upgrade SonarQube plugin".
- After the prefix, use the imperative mood with a capitalized first word.
- Combine closely related changes as separate sentences in the subject.
- Add a body (after a blank line) only when the change needs a rationale; state the why, not the diff.

## Branch names

- `<category>/<kebab-case-description>`. Example: `project/upgrade-gradle`.
- Leave `dependabot/...` branches to Dependabot.

## Pull request names

- Same as the branch name.

## Handling Copilot comments

- Do not apply Copilot's suggestions automatically.
- Review each comment, plus any "comments suppressed due to low confidence" note, and decide whether it is worth fixing or a false positive.
- Propose how a fix would look and wait for the user's approval before changing anything.
- Once approved: apply the fix, run the build to test it, commit and push, then reply to the comment and mark the thread resolved.
