#include "Script.hpp"

#include "quartz/Bind.hpp"
#include "quartz/Quartz.hpp"

#include <algorithm>
#include <array>
#include <cstdio>
#include <format>
#include <fstream>
#include <iterator>
#include <print>

namespace arena {

namespace {

using quartz::vm::HostCall;
using quartz::vm::Slot;

std::string readFile(const std::filesystem::path& path) {
    std::ifstream in(path, std::ios::binary);
    return {std::istreambuf_iterator<char>(in), std::istreambuf_iterator<char>()};
}

World& worldOf(void* user) { return *static_cast<World*>(user); }

std::string_view text(const Slot& slot) { return static_cast<const quartz::vm::Str*>(slot.p)->view(); }

// A handle Quartz passes back is only a number: the host decides whether it still means something.
Entity* entityArg(HostCall& call, void* user) {
    Entity* e = worldOf(user).find(call.args[0].u);
    if (e == nullptr) {
        call.error = std::format("no entity has handle {}", call.args[0].u);
    }
    return e;
}

template <auto Read>
bool getter(HostCall& call, void* user) {
    Entity* e = entityArg(call, user);
    if (e != nullptr) {
        Read(*e, call);
    }
    return e != nullptr;
}

template <auto Write>
bool setter(HostCall& call, void* user) {
    Entity* e = entityArg(call, user);
    if (e != nullptr) {
        Write(worldOf(user), *e, call.args[1]);
    }
    return e != nullptr;
}

double clampTo(double v, int limit) { return std::clamp(v, 0.0, static_cast<double>(limit - 1)); }

using EntityHandle = quartz::HandleOf<"Entity">;

EntityHandle player(World* world) { return {world->player().id}; }

void despawn(HostCall& call, World* world, EntityHandle handle) {
    Entity* e = world->find(handle.id);
    if (e == nullptr) {
        call.error = std::format("no entity has handle {}", handle.id);
    } else if (e->kind != "player") {
        e->alive = false;
    }
}

std::int64_t count(World* world, std::string_view kind) { return world->count(kind); }

double width(World* world) { return world->width(); }

double height(World* world) { return world->height(); }

} // namespace

Script::Script(World& world, std::filesystem::path interface, std::filesystem::path source, std::int64_t fuel)
    : world_(world), interface_(std::move(interface)), source_(std::move(source)), fuel_(fuel) {}

Script::~Script() = default;

std::shared_ptr<const quartz::vm::Program> Script::compile() {
    std::error_code ec;
    stamp_ = std::filesystem::last_write_time(source_, ec);
    std::array<quartz::SourceInput, 2> inputs{{
        {interface_.filename().string(), readFile(interface_)},
        {source_.filename().string(), readFile(source_)},
    }};
    quartz::CompileOutput out = quartz::compile(inputs);
    if (!out.diagnostics.empty()) {
        std::fputs(out.diagnostics.c_str(), stderr);
    }
    return out.program;
}

void Script::bindHost(quartz::vm::Sandbox& sandbox) {
    void* w = &world_;
    sandbox.bind("Entity.id", getter<[](Entity& e, HostCall& c) { c.result.i = static_cast<std::int64_t>(e.id); }>, w);
    sandbox.bind("Entity.kind", getter<[](Entity& e, HostCall& c) { c.result.p = c.string(e.kind); }>, w);
    sandbox.bind("Entity.alive", getter<[](Entity& e, HostCall& c) { c.result.u = e.alive ? 1 : 0; }>, w);
    sandbox.bind("Entity.x", getter<[](Entity& e, HostCall& c) { c.result.f = e.x; }>, w);
    sandbox.bind("Entity.y", getter<[](Entity& e, HostCall& c) { c.result.f = e.y; }>, w);
    sandbox.bind("Entity.hp", getter<[](Entity& e, HostCall& c) { c.result.i = e.hp; }>, w);
    sandbox.bind("Entity.setX",
                 setter<[](World& world, Entity& e, const Slot& v) { e.x = clampTo(v.f, world.width()); }>, w);
    sandbox.bind("Entity.setY",
                 setter<[](World& world, Entity& e, const Slot& v) { e.y = clampTo(v.f, world.height()); }>, w);
    sandbox.bind("Entity.setHp", setter<[](World&, Entity& e, const Slot& v) { e.hp = v.i; }>, w);
    sandbox.bind(
        "Entity.say",
        setter<[](World&, Entity& e, const Slot& v) { std::println("  {}#{} says: {}", e.kind, e.id, text(v)); }>, w);
    sandbox.bind(
        "spawn",
        [](HostCall& c, void* user) {
            World& world = worldOf(user);
            // Spawning allocates host memory, so it costs the script fuel like its own allocations do.
            if (!c.charge(100)) {
                return false;
            }
            Entity& e =
                world.spawn(text(c.args[0]), clampTo(c.args[1].f, world.width()), clampTo(c.args[2].f, world.height()));
            c.result.u = e.id;
            return true;
        },
        w);
    // Typed bindings: the C++ signature is checked against the interface, and the world comes as the user pointer.
    for (auto bound :
         {quartz::bind<&player>(sandbox, "player", &world_), quartz::bind<&despawn>(sandbox, "despawn", &world_),
          quartz::bind<&count>(sandbox, "count", &world_), quartz::bind<&width>(sandbox, "width", &world_),
          quartz::bind<&height>(sandbox, "height", &world_)}) {
        if (!bound) {
            report("bind", bound.error());
        }
    }
}

bool Script::load() {
    program_ = compile();
    if (!program_) {
        return false;
    }
    quartz::vm::Limits limits;
    limits.memoryBytes = 8u << 20;
    limits.fuelPerCall = fuel_;
    sandbox_ = std::make_unique<quartz::vm::Sandbox>(program_, limits);
    bindHost(*sandbox_);
    if (auto started = sandbox_->start(); !started) {
        report("start", started.error().describe());
        return false;
    }
    return true;
}

void Script::reloadIfChanged() {
    std::error_code ec;
    auto now = std::filesystem::last_write_time(source_, ec);
    if (ec || now == stamp_) {
        return;
    }
    std::println("== {} changed, recompiling", source_.filename().string());
    auto next = compile();
    if (!next) {
        std::println("== the old version keeps running");
        return;
    }
    auto fresh = sandbox_->reload(next);
    if (!fresh) {
        report("reload", fresh.error().describe());
        return;
    }
    program_ = next;
    sandbox_ = std::move(*fresh);
    std::println("== reloaded, state kept: {}", state());
}

void Script::tick(std::int64_t tick) {
    Slot arg{};
    arg.i = tick;
    if (auto r = sandbox_->call(program_->findFunction("tick"), std::span(&arg, 1)); !r) {
        report("tick", r.error().describe());
    }
    std::uint32_t think = program_->findFunction("think");
    for (std::uint64_t id : world_.living()) {
        Slot entity{};
        entity.u = id;
        if (auto r = sandbox_->call(think, std::span(&entity, 1)); !r) {
            report(std::format("think for entity {}", id), r.error().describe());
        }
    }
    world_.sweep();
}

std::string Script::state() {
    auto json = sandbox_->exportState();
    return json ? *json : json.error().describe();
}

void Script::report(std::string_view what, const std::string& why) {
    std::println("!! {} trapped, the world goes on: {}", what, why);
}

} // namespace arena
