#include "World.hpp"

#include <algorithm>
#include <cmath>

namespace arena {

World::World(int width, int height) : width_(width), height_(height) { spawn("player", width / 2.0, height / 2.0); }

Entity& World::spawn(std::string_view kind, double x, double y) {
    entities_.push_back(Entity{.id = nextId_++, .kind = std::string(kind), .x = x, .y = y});
    return entities_.back();
}

Entity* World::find(std::uint64_t id) {
    auto it = std::ranges::find(entities_, id, &Entity::id);
    return it == entities_.end() ? nullptr : &*it;
}

std::int64_t World::count(std::string_view kind) const {
    return std::ranges::count_if(entities_, [&](const Entity& e) { return e.alive && e.kind == kind; });
}

std::vector<std::uint64_t> World::living() const {
    std::vector<std::uint64_t> ids;
    for (const Entity& e : entities_) {
        if (e.alive) {
            ids.push_back(e.id);
        }
    }
    return ids;
}

void World::sweep() {
    // The player stays at index 0 forever, so player() never dangles.
    std::erase_if(entities_, [](const Entity& e) { return !e.alive && e.kind != "player"; });
}

std::string World::render() const {
    std::string frame;
    std::string border = "+" + std::string(static_cast<std::size_t>(width_), '-') + "+\n";
    std::vector<std::string> rows(static_cast<std::size_t>(height_),
                                  std::string(static_cast<std::size_t>(width_), ' '));
    for (const Entity& e : entities_) {
        if (!e.alive) {
            continue;
        }
        auto col = static_cast<int>(std::lround(e.x));
        auto row = static_cast<int>(std::lround(e.y));
        if (col < 0 || row < 0 || col >= width_ || row >= height_) {
            continue;
        }
        char glyph = e.kind == "player" ? '@' : e.kind.empty() ? '?' : e.kind[0];
        rows[static_cast<std::size_t>(row)][static_cast<std::size_t>(col)] = glyph;
    }
    frame += border;
    for (const std::string& row : rows) {
        frame += "|" + row + "|\n";
    }
    frame += border;
    return frame;
}

} // namespace arena
