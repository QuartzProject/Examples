#pragma once

#include "World.hpp"

#include <cstdint>
#include <filesystem>
#include <memory>
#include <string>

namespace quartz::vm {
class Program;
class Sandbox;
} // namespace quartz::vm

namespace arena {

/// One Quartz script driving the world: compiled from disk, recompiled and reloaded when the file changes.
class Script {
public:
    Script(World& world, std::filesystem::path interface, std::filesystem::path source, std::int64_t fuel);
    ~Script();

    /// Compiles and starts the script; false (with the reason printed) when it does not compile.
    bool load();
    /// Reloads the script if its file changed, keeping every `state`; a broken edit leaves the old one running.
    void reloadIfChanged();
    void tick(std::int64_t tick);
    std::string state();

private:
    std::shared_ptr<const quartz::vm::Program> compile();
    void bindHost(quartz::vm::Sandbox& sandbox);
    void report(std::string_view what, const std::string& why);

    World& world_;
    std::filesystem::path interface_;
    std::filesystem::path source_;
    std::filesystem::file_time_type stamp_{};
    std::int64_t fuel_;
    std::shared_ptr<const quartz::vm::Program> program_;
    std::unique_ptr<quartz::vm::Sandbox> sandbox_;
};

} // namespace arena
