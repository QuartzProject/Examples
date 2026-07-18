#pragma once

#include <cstdint>
#include <string>
#include <string_view>
#include <vector>

namespace arena {

struct Entity {
    std::uint64_t id = 0;
    std::string kind;
    double x = 0;
    double y = 0;
    std::int64_t hp = 10;
    bool alive = true;
};

/// The game state the C++ side owns: Quartz code sees entities only as opaque handles.
class World {
public:
    World(int width, int height);

    Entity& spawn(std::string_view kind, double x, double y);
    /// The entity behind a handle, or null when no entity ever had that id.
    Entity* find(std::uint64_t id);

    Entity& player() { return entities_.front(); }

    std::int64_t count(std::string_view kind) const;
    /// Ids of the living entities, copied so scripts can spawn and despawn while the host walks them.
    std::vector<std::uint64_t> living() const;
    void sweep();

    int width() const { return width_; }

    int height() const { return height_; }

    std::string render() const;

private:
    int width_;
    int height_;
    std::uint64_t nextId_ = 1;
    std::vector<Entity> entities_;
};

} // namespace arena
