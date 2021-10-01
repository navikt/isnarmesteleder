# isnarmesteleder
Applikasjon for å vise informasjon relasjoner mellom ansatte og deres narmeste leder. Relasjonene lese og replikeres fra applikasjonen Narmesteleder og aggregeres for bruk i Sykefraværsoppfølgingen.

## Technologies used

* Docker
* Gradle
* Kotlin
* Kafka
* Ktor
* Postgres
* Redis

##### Test Libraries:

* Kluent
* Mockk
* Spek

#### Requirements

* JDK 11

### Build

Run `./gradlew clean shadowJar`

### Lint (Ktlint)

##### Command line

Run checking: `./gradlew --continue ktlintCheck`

Run formatting: `./gradlew ktlintFormat`

##### Git Hooks

Apply checking: `./gradlew addKtlintCheckGitPreCommitHook`

Apply formatting: `./gradlew addKtlintFormatGitPreCommitHook`

### Test

Run `./gradlew test -i`

### Run Application

#### Create Docker Image

Creating a docker image should be as simple as `docker build -t isdialogmote .`

#### Run Docker Image

`docker run --rm -it -p 8080:8080 isdialogmote`

### Cache

This application uses Redis for caching. Redis is deployed automatically on changes to workflow or config on master
branch. For manual deploy, run: `kubectl apply -f .nais/redis-config.yaml`
or `kubectl apply -f .nais/redisexporter.yaml`.

### Kafka

This application consumes the following topic(s):

* teamsykmelding.syfo-narmesteleder-leesah
