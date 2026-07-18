/* A fleet of ships steered by a Quartz script through the C interface, reloaded when the script changes. */
#include <quartz/quartz.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <time.h>

#define SHIPS 4

typedef struct {
    double x, y;
} Ship;

typedef struct {
    double beaconX, beaconY;
    Ship ships[SHIPS];
} Fleet;

static char* read_file(const char* path, size_t* length) {
    FILE* f = fopen(path, "rb");
    if (f == NULL) {
        return NULL;
    }
    fseek(f, 0, SEEK_END);
    long size = ftell(f);
    fseek(f, 0, SEEK_SET);
    char* text = malloc((size_t)size + 1);
    *length = fread(text, 1, (size_t)size, f);
    text[*length] = '\0';
    fclose(f);
    return text;
}

static long long modified(const char* path) {
    struct stat st;
    return stat(path, &st) == 0 ? (long long)st.st_mtim.tv_sec * 1000000000LL + st.st_mtim.tv_nsec : -1;
}

/* Compiles the host interface and the script together; NULL (after printing why) when it does not compile. */
static qz_program* compile(const char* interface_path, const char* script_path) {
    size_t lengths[2] = {0, 0};
    char* texts[2] = {read_file(interface_path, &lengths[0]), read_file(script_path, &lengths[1])};
    qz_program* program = NULL;
    if (texts[0] != NULL && texts[1] != NULL) {
        qz_source sources[2] = {{"fleet.qzi", texts[0], lengths[0]}, {"pilot.qz", texts[1], lengths[1]}};
        qz_compile_options options = {.keep_lines = true};
        char* diagnostics = NULL;
        program = qz_compile(sources, 2, &options, &diagnostics);
        if (diagnostics != NULL) {
            fputs(diagnostics, stderr);
            qz_free(diagnostics);
        }
    } else {
        fprintf(stderr, "fleet: cannot read %s or %s\n", interface_path, script_path);
    }
    free(texts[0]);
    free(texts[1]);
    return program;
}

static bool target_x(qz_call* call, const qz_slot* args, qz_slot* result, void* user) {
    (void)call, (void)args;
    result->f = ((Fleet*)user)->beaconX;
    return true;
}

static bool target_y(qz_call* call, const qz_slot* args, qz_slot* result, void* user) {
    (void)call, (void)args;
    result->f = ((Fleet*)user)->beaconY;
    return true;
}

static bool radio(qz_call* call, const qz_slot* args, qz_slot* result, void* user) {
    (void)call, (void)result, (void)user;
    size_t length = 0;
    const char* text = qz_str_at(&args[1], &length);
    printf("  ship %lld: %.*s\n", (long long)args[0].i, (int)length, text);
    return true;
}

static void step(qz_sandbox* sandbox, uint32_t steer, Fleet* fleet) {
    for (int i = 0; i < SHIPS; ++i) {
        qz_slot args[3];
        args[0].i = i;
        args[1].f = fleet->ships[i].x;
        args[2].f = fleet->ships[i].y;
        qz_slot thrust[2];
        if (qz_sandbox_call_wide(sandbox, steer, args, 3, thrust, 2) != QZ_OK) {
            const char* kind = qz_sandbox_trap_code(sandbox) == QZ_TRAP_OUT_OF_FUEL ? "ran out of fuel" : "trapped";
            printf("!! steering ship %d %s, it drifts: %s\n", i, kind, qz_sandbox_error(sandbox));
            continue;
        }
        fleet->ships[i].x += thrust[0].f;
        fleet->ships[i].y += thrust[1].f;
    }
}

static void print_state(qz_sandbox* sandbox) {
    char* json = qz_sandbox_export_state(sandbox);
    printf("state: %s\n", json != NULL ? json : qz_sandbox_error(sandbox));
    qz_free(json);
}

int main(int argc, char** argv) {
    const char* script = argc > 1 ? argv[1] : "scripts/pilot.qz";
    const char* interface = argc > 2 ? argv[2] : "scripts/fleet.qzi";
    int ticks = argc > 3 ? atoi(argv[3]) : 60;
    if (argc > 4 || ticks <= 0) {
        fprintf(stderr, "usage: fleet [script.qz] [interface.qzi] [ticks]\n");
        return 2;
    }

    Fleet fleet = {.beaconX = 20, .beaconY = 10, .ships = {{0, 0}, {40, 0}, {0, 20}, {35, 18}}};
    qz_program* program = compile(interface, script);
    if (program == NULL) {
        return 1;
    }
    qz_limits limits = {.memory_bytes = 4u << 20, .fuel_per_call = 20000};
    qz_sandbox* sandbox = qz_sandbox_new(program, &limits);
    qz_sandbox_bind_direct(sandbox, "targetX", target_x, &fleet);
    qz_sandbox_bind_direct(sandbox, "targetY", target_y, &fleet);
    qz_sandbox_bind_direct(sandbox, "radio", radio, &fleet);
    if (qz_sandbox_start(sandbox) != QZ_OK) {
        fprintf(stderr, "fleet: %s\n", qz_sandbox_error(sandbox));
        return 1;
    }
    long long stamp = modified(script);

    for (int tick = 1; tick <= ticks; ++tick) {
        long long now = modified(script);
        if (now != stamp) {
            stamp = now;
            qz_program* next = compile(interface, script);
            if (next == NULL) {
                printf("== %s does not compile, the old version keeps flying\n", script);
            } else if (qz_sandbox_reload(sandbox, next) != QZ_OK) {
                printf("== reload refused, the old version keeps flying: %s\n", qz_sandbox_error(sandbox));
                qz_program_free(next);
            } else {
                qz_program_free(program);
                program = next;
                printf("== reloaded %s\n", script);
                print_state(sandbox);
            }
        }
        if (tick == ticks / 2) {
            fleet.beaconX = 5;
            fleet.beaconY = 5;
            printf("-- tick %d: the beacon moves to (5, 5)\n", tick);
        }
        step(sandbox, qz_program_function(program, "steer"), &fleet);
        if (tick % 10 == 0) {
            printf("tick %3d:", tick);
            for (int i = 0; i < SHIPS; ++i) {
                printf(" (%5.1f, %5.1f)", fleet.ships[i].x, fleet.ships[i].y);
            }
            printf("\n");
        }
        fflush(stdout);
        struct timespec pause = {0, 50 * 1000000L};
        nanosleep(&pause, NULL);
    }
    print_state(sandbox);
    qz_sandbox_free(sandbox);
    qz_program_free(program);
    return 0;
}
