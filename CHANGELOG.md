# Changelog

## 1.0.8+1.21.1 (hotfix, version unchanged)

- Fix: first join of a singleplayer world after game start kicked the player with
  "Invalid player data" (`ArrayIndexOutOfBoundsException: Index 21 out of bounds for length 21`
  in `Player.defineSynchedData`, plus the `defineId called for: class Player from class
  RemoteCastDataKeys` warning). The remote-cast execution colour `EntityDataAccessor` was
  registered lazily from `RemoteCastDataKeys` while the first `Player` was already being
  constructed. It is now a `@Unique static final` field in `MixinPlayerSyncedData`, so it is
  registered during `Player`'s static init; other code reads/writes it through the
  `RemoteCastColorHolder` duck interface.
