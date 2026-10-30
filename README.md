![FoChatGames cover](docs/assets/cover.png)

# FoChatGames

[![Modrinth](https://img.shields.io/badge/Download-Modrinth-1bd96a?logo=modrinth&logoColor=white)](https://modrinth.com/plugin/fochatgames) [![Versions](https://img.shields.io/badge/Supported_versions-Modrinth-1bd96a)](https://modrinth.com/plugin/fochatgames/versions) [![Discord](https://img.shields.io/badge/Support-Discord-5865f2?logo=discord&logoColor=white)](https://discord.gg/kEF77QCfu7)

Automated chat games with rewards, statistics, controls, and extension events.

*Developed by Carrotio.*

## Features

- Runs automatic chat games for completion, maths, trivia, typing, reversal, unscrambling, and number guessing.
- Supports configurable schedules, rewards, broadcasts, and world exclusions.
- Tracks wins, per-game records, and player preferences in persistent storage.
- Provides administration tools for manual games, logs, statistics, and resets.
- Exposes cancellable Bukkit events and PlaceholderAPI statistics.

## Download and dependencies

Download the latest compatible version from [Modrinth](https://modrinth.com/plugin/fochatgames). Check the [versions page](https://modrinth.com/plugin/fochatgames/versions) for supported Minecraft versions and server platforms.

- **Required:** None
- **Optional integrations:** `PlaceholderAPI`

## Screenshots

![Open the chat games administrator editor.](docs/assets/screenshots/fochatgames-admin-editor.png)

*Open the chat games administrator editor.*

![Manage the available game types.](docs/assets/screenshots/fochatgames-game-types.png)

*Manage the available game types.*

![Configure general game behavior.](docs/assets/screenshots/fochatgames-general-settings.png)

*Configure general game behavior.*

![Control excluded worlds.](docs/assets/screenshots/fochatgames-world-blacklist.png)

*Control excluded worlds.*

## Support

For help or issue reports, join the [Carrotio Discord server](https://discord.gg/kEF77QCfu7).

<details>
<summary><strong>Commands and permissions</strong></summary>

### Commands

- `/fochatgames <help|statistics|start|custom|stop|stopall|toggle|status|logs|reset>` — FoChatGames commands Aliases: `/chatgames`, `/chatgame`, `/focg`, `/cg`.
- `/fochatgamesadmin <version|reload|editor>` — FoChatGames commands Aliases: `/chatgamesadmin`.

### Permissions

- `fochatgames.admin` — Access FoChatGames admin command and update notices. Default: `op`.
- `fochatgames.help` — View FoChatGames help. Default: `true`.
- `fochatgames.start` — Start chat games manually. Default: `op`.
- `fochatgames.custom` — Start custom chat games. Default: `op`.
- `fochatgames.stop` — Stop active chat games. Default: `op`.
- `fochatgames.stopall` — Stop all active chat games. Default: `op`.
- `fochatgames.statistics` — View own statistics. Default: `true`.
- `fochatgames.statistics.others` — View other player statistics. Default: `op`.
- `fochatgames.toggle` — Toggle chat game messages and sounds. Default: `true`.
- `fochatgames.status` — View chat game status. Default: `op`.
- `fochatgames.logs` — View win logs. Default: `op`.
- `fochatgames.reset` — Reset player stats. Default: `op`.

</details>

<details>
<summary><strong>PlaceholderAPI</strong></summary>

Requires PlaceholderAPI.

- `%fochatgames_next% / %fochatgames_seconds_until_next%` — Seconds until the next automatic game.
- `%fochatgames_active%` — Whether a game is active.
- `%fochatgames_active_game%` — Key of the active game.
- `%fochatgames_enabled%` — Whether the player receives chat games.
- `%fochatgames_wins% / %fochatgames_wins_raw%` — Player's total wins; aliases include won, chatgames_won, total_wins, and total_won.
- `%fochatgames_record% / %fochatgames_record_raw%` — Player's best record; aliases include best_record and chatgames_record.
- `%fochatgames_wins_<game>% / %fochatgames_wins_<game>_raw%` — Wins for completion, guess_the_number, math, trivia, type, unreverse, or unscramble. Reverse aliases are also accepted.
- `%fochatgames_record_<game>% / %fochatgames_record_<game>_raw%` — Best record for a specific game. Reverse aliases are also accepted.

</details>

<details>
<summary><strong>Configuration</strong></summary>

- `config.yml` — Main behavior and feature settings.
- `messages.yml` — Player-facing messages and interface text.
- `sounds.yml` — Configurable interaction sounds.
- `games/*.yml` — Individual chat-game definitions.

Main config areas: `general`, `native-dialogs`, `world-blacklist`, `rewards`, `broadcasts`.

</details>

<details>
<summary><strong>Developer API</strong></summary>

### Maven dependency example

Install the matching `FoChatGames-1.7.jar` locally if it is not in your Maven repository. These coordinates do not claim a published artifact. Use `provided` scope so the server supplies FoChatGames:

```xml
<dependency>
  <groupId>me.foesio</groupId>
  <artifactId>FoChatGames</artifactId>
  <version>1.7</version>
  <scope>provided</scope>
</dependency>
```

Compile against the matching FoChatGames jar as a `provided` Maven dependency (or
`compileOnly` in Gradle). Do not shade it into the consumer. FoChatGames is not
published as a Maven artifact; install the matching jar locally or use a local
file dependency. Declare `depend: [FoChatGames]` in a `plugin.yml` consumer, or
`softdepend: [FoChatGames]` if the integration can run without it. Keep direct
FoChatGames class references in a separate integration class for optional use.

For a local Maven build, install the matching jar with
`mvn install:install-file -Dfile=FoChatGames-1.7.jar -DgroupId=me.foesio -DartifactId=FoChatGames -Dversion=1.7 -Dpackaging=jar`,
then declare `me.foesio:FoChatGames:1.7` with `provided` scope.

Find the two services after FoChatGames enables:

~~~java
ChatGamesService reads = Bukkit.getServicesManager().load(ChatGamesService.class);
ChatGameControlService control = Bukkit.getServicesManager().load(ChatGameControlService.class);
if (reads != null && control != null) {
    reads.activeGame().ifPresent(game -> getLogger().info(game.gameKey()));
    ChatGameControlService.StartResult result = control.start("trivia");
}
~~~

`ChatGamesService` returns an immutable active-game snapshot without the answer,
seconds until the next automatic attempt, and stored numeric win counts and best
times for a UUID. An absent game is `Optional.empty()`; missing stats are zero.
`ChatGameControlService` starts a weighted random, named, or custom game and can
silently stop the active game. Its result enums distinguish success, no-op,
invalid input, cancellation, and unavailable provider. Manual starts follow
`/chatgames start` semantics: they bypass the automatic enabled and minimum-player
checks. The caller is responsible for its own permissions.

All service methods must be called on the Bukkit server thread. Off-thread calls
throw `IllegalStateException`. Read methods also throw if a cached service is
used after FoChatGames disables; control methods return `UNAVAILABLE`. Services
are registered after startup and removed on disable. Reacquire them after a
provider restart. This API does not promise Folia region-thread support.

The existing `ChatGameStartEvent` and `ChatGameWinEvent` are cancellable *before*
events. `ChatGameStartEvent` fires before the game becomes active;
`ChatGameWinEvent` fires before stats, rewards, and closure. `ChatGameEndEvent`
fires after timeout or manual stop, never after a win. Their existing signatures
and behavior remain available.

`ChatGameStartedEvent` fires after an accepted game is active and its timeout is
scheduled. `ChatGameFinishedEvent` fires after an accepted win, timeout, or manual
stop. Its outcome, winner UUID/name (win only), elapsed time, and `statsPersisted`
describe the result. A win's `statsPersisted` is true only when both win stats
and the win log committed. Reward commands and item delivery have separate
failure behavior; the finished event does not guarantee reward delivery. Both
new events are non-cancellable and synchronous on the Bukkit server thread.
Snapshots and stats maps are immutable. The snapshot's UUID correlates a start
with its finish. Cancelled starts and wins produce no new result event.

~~~java
@EventHandler
public void onChatGameFinished(ChatGameFinishedEvent event) {
    if (event.outcome() == ChatGameFinishedEvent.Outcome.WIN && event.statsPersisted()) {
        UUID winner = event.winnerId();
    }
}
~~~

### Complete public API method index

Signatures use short type names; import Fo types from this plugin's `api` package and Bukkit types from Bukkit.

#### `ChatGameControlService`

```java
ChatGameControlService.StartResult startRandom();
ChatGameControlService.StartResult start(String);
ChatGameControlService.StartResult startCustom(String, List<String>);
ChatGameControlService.StopResult stop();
```

#### `ChatGamesService`

```java
Optional<ChatGameSnapshot> activeGame();
int secondsUntilNext();
ChatGameStats stats(UUID);
```

**Other public contract types:** `ChatGameSnapshot`, `ChatGameStats`.

### Complete Bukkit event index

- `ChatGameEndEvent`.
- `ChatGameFinishedEvent`.
- `ChatGameStartEvent`.
- `ChatGameStartedEvent`.
- `ChatGameWinEvent`.

</details>
