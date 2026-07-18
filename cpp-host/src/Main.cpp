#include "Script.hpp"
#include "World.hpp"

#include <charconv>
#include <chrono>
#include <cstdio>
#include <print>
#include <string_view>
#include <thread>

namespace {

struct Options {
    std::filesystem::path script = "scripts/brain.qz";
    std::int64_t ticks = -1;
    std::int64_t tickMillis = 100;
    std::int64_t renderEvery = 10;
    std::int64_t fuel = 200'000;
};

constexpr std::string_view kUsage = R"(usage: arena [options] [script.qz]

Runs a small arena whose entities are driven by a Quartz script (scripts/brain.qz by default). Edit the script
while the arena runs: it is recompiled and reloaded, and its `state` survives the reload.

  --ticks N          stop after N ticks (default: run until interrupted)
  --tick-ms N        milliseconds per tick (default 100; 0 runs as fast as possible)
  --render-every N   draw the arena every N ticks (default 10; 0 never)
  --fuel N           fuel budget of each call into the script (default 200000)
)";

bool number(std::string_view text, std::int64_t& out) {
    auto [end, ec] = std::from_chars(text.data(), text.data() + text.size(), out);
    return ec == std::errc{} && end == text.data() + text.size() && out >= 0;
}

int parse(int argc, char** argv, Options& o) {
    for (int i = 1; i < argc; ++i) {
        std::string_view arg = argv[i];
        std::int64_t* target = arg == "--ticks"          ? &o.ticks
                               : arg == "--tick-ms"      ? &o.tickMillis
                               : arg == "--render-every" ? &o.renderEvery
                               : arg == "--fuel"         ? &o.fuel
                                                         : nullptr;
        if (arg == "-h" || arg == "--help") {
            std::print("{}", kUsage);
            return 0;
        }
        if (target != nullptr) {
            if (i + 1 >= argc || !number(argv[i + 1], *target)) {
                std::print(stderr, "arena: {} needs a non-negative number\n", arg);
                return 2;
            }
            ++i;
        } else if (arg.starts_with("-")) {
            std::print(stderr, "arena: unknown option {}\n{}", arg, kUsage);
            return 2;
        } else {
            o.script = arg;
        }
    }
    return -1;
}

} // namespace

int main(int argc, char** argv) {
    Options options;
    if (int code = parse(argc, argv, options); code >= 0) {
        return code;
    }
    arena::World world(48, 14);
    arena::Script script(world, options.script.parent_path() / "game.qzi", options.script, options.fuel);
    if (!script.load()) {
        return 1;
    }
    for (std::int64_t tick = 1; options.ticks < 0 || tick <= options.ticks; ++tick) {
        script.reloadIfChanged();
        script.tick(tick);
        if (options.renderEvery > 0 && tick % options.renderEvery == 0) {
            std::print("tick {}, player hp {}\n{}", tick, world.player().hp, world.render());
        }
        std::fflush(stdout);
        if (options.tickMillis > 0) {
            std::this_thread::sleep_for(std::chrono::milliseconds(options.tickMillis));
        }
    }
    std::println("final state: {}", script.state());
    return 0;
}
