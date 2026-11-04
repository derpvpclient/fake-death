# Fake Death

A convincing fake death for Minecraft 26.3 (Fabric): a short scene that fits the moment, the real death screen and a real death message in chat. But you are still alive.

Everything happens on your client only. The server notices nothing, other players see nothing, and no operator permissions are needed.

- Download: [Modrinth](https://modrinth.com/mod/fake-death)
- Commands: `/fakedeath`, `/fakedeath test <scene>`, `/fakedeath in <seconds>`, `/fakedeath random on|off`
- Settings: Options → "Fake Death..." or Mod Menu

## Building

Needs Java 25. Put the jar listed in [libs/README.md](libs/README.md) into `libs/`, then:

```
./gradlew build
```

The mod jar lands in `build/libs/`.

## License

MIT, see [LICENSE](LICENSE).
