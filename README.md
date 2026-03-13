<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/QuartzProject/.github/main/.github/assets/quartz-logo-rose.svg">
    <img src="https://raw.githubusercontent.com/QuartzProject/.github/main/.github/assets/quartz-logo-ink-rose.svg" alt="Quartz" width="360">
  </picture>
</p>

<h2 align="center">Quartz Examples</h2>

<p align="center">
  <strong>Runnable projects that use Quartz.</strong>
</p>

<p align="center">
  <img alt="Quartz" src="https://img.shields.io/badge/language-Quartz-F9A8D4">
  <img alt="Build" src="https://img.shields.io/badge/build-CMake_%7C_Gradle-064F8C?logo=cmake&logoColor=white">
  <img alt="License" src="https://img.shields.io/badge/license-MIT-green">
</p>

## About

| Example | What it is |
|---|---|
| [`paper-plugin/`](paper-plugin) | A Paper plugin whose commands and listeners are written in Quartz, built with [Embed](https://github.com/QuartzProject/Embed) |
| [`cli-tool/`](cli-tool) | `qlog`, a log analyzer built as one native executable |
| [`cpp-host/`](cpp-host) | A terminal arena driven by a script, through the C++ API |
| [`c-host/`](c-host) | A fleet of ships steered by a script, through the C API |
| [`language/`](language) | Small programs that show the language |

Each one is explained in the [Examples chapter](https://quartzlang.org/docs/examples.html) of the Quartz book.

## Building

The builds look for checkouts of [Quartz](https://github.com/QuartzProject/Quartz) and
[Embed](https://github.com/QuartzProject/Embed) next to this repository. The native examples need Clang with C++23,
CMake 3.28+ and Ninja; the Paper plugin needs a JDK 25.

```bash
cmake -S cpp-host -B build/arena -G Ninja && cmake --build build/arena
```

## License

[MIT](LICENSE). The Paper plugin is a port of
[MawuExample-Paper](https://github.com/BluevaDevelopment/MawuExample-Paper), relicensed as MIT by its author.
