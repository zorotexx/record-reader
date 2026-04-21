package ru.zorotexx.recordReader.reader;

import dev.by1337.core.particle.ParticleRenderUtil;
import dev.by1337.core.util.misc.Pair;
import dev.by1337.particle.ParticleRender;
import dev.by1337.particle.ParticleType;
import dev.by1337.particle.particle.PacketBuilder;
import dev.by1337.particle.particle.ParticleData;
import dev.by1337.particle.particle.ParticleOption;
import dev.by1337.particle.particle.ParticleSource;
import dev.by1337.particle.particle.options.BlockParticleOption;
import dev.by1337.particle.particle.options.DustParticleOptions;
import dev.by1337.particle.particle.options.ItemParticleOption;
import dev.by1337.yaml.codec.YamlCodec;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.entity.Player;
import ru.zorotexx.recordReader.utils.Vec3d;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class ParticleRecordReader {
    public static ParticleSpawnerData read(File file) {
        try {
            return read(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public record ParticleSpawnerData(long end, Long2ObjectOpenHashMap<ParticleSource> particles) {
        public static ParticleSpawnerData EMPTY = new ParticleSpawnerData(0, new Long2ObjectOpenHashMap<>());
        public ParticleSpawner createSpawner(double x, double y, double z) {
            return new ParticleSpawner(this, x, y, z);
        }
    }

    public static class ParticleSpawner {
        public static ParticleSpawner EMPTY = new ParticleSpawner(new ParticleSpawnerData(0, new Long2ObjectOpenHashMap<>()), 0, 0, 0);
        private final ParticleSpawnerData data;
        private long tick;
        private final double x;
        private final double y;
        private final double z;

        public ParticleSpawner(ParticleSpawnerData data, double x, double y, double z) {
            this.data = data;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void run(Collection<Player> players) {
            var s = data.particles().get(tick);
            if (s != null) {
                ParticleRender.render(players,
                        s,
                        x,
                        y,
                        z
                );
            }
            tick++;
            if (tick >= data.end) {
                tick = 0;
            }
        }
    }


    public static ParticleSpawnerData read(List<String> lines) {
        //schema=tick,type,pos,dist,speed,count,alwaysShow,extra_type,r,g,b,size
        BySchemaParser parser = new BySchemaParser();
        long minimum = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        Vec3d offsets = new Vec3d(0, 0, 0);
        Long2ObjectOpenHashMap<List<Pair<Vec3d, ParticleData>>> particles = new Long2ObjectOpenHashMap<>();
        for (String line : lines) {
            if (line.isEmpty()) continue;
            if (line.startsWith("schema=")) {
                parser.setSchema(line.substring("schema=".length()));
                continue;
            }
            if (line.startsWith("#schema=")) {
                parser.setSchema(line.substring("#schema=".length()));
                continue;
            }
            parser.setValues(line);
            String raw_type = parser.decode("type", YamlCodec.STRING);
            if (raw_type.equals("offsets")) {
                offsets = parser.decode("offsets", Vec3d.CODEC);
                continue;
            }
            long tick = parser.decode("tick", YamlCodec.LONG);
            ParticleType type = parser.decode("type", ParticleRenderUtil.PARTICLE_TYPE_CODEC);
            Vec3d pos = parser.decode("pos", Vec3d.CODEC).sub(offsets);
            Vec3d dist = parser.decode("dist", Vec3d.CODEC);
            float speed = parser.decode("speed", YamlCodec.FLOAT);
            int count = parser.decode("count", YamlCodec.INT);
            boolean alwaysShow = parser.decode("alwaysShow", YamlCodec.BOOL);
            String extra = parser.decode("extra_type", YamlCodec.STRING);
            ParticleOption data;
            if ("default".equals(extra)) {
                data = null;
            } else if ("block".equals(extra)) {
                data = new BlockParticleOption(parser.decode("block", ParticleRenderUtil.BLOCK_TYPE_CODEC));
            } else if ("dust".equals(extra)) {
                //r,g,b,size
                data = new DustParticleOptions(
                        parser.decode("r", YamlCodec.FLOAT),
                        parser.decode("g", YamlCodec.FLOAT),
                        parser.decode("b", YamlCodec.FLOAT),
                        parser.decode("size", YamlCodec.FLOAT)
                );
            } else if ("item".equals(extra)) {
                data = new ItemParticleOption(parser.decode("item", ParticleRenderUtil.ITEM_TYPE_CODEC));
            } else {
                throw new IllegalStateException("unknown " + extra);
            }
            minimum = Math.min(tick, minimum);
            max = Math.max(tick, max);
            var v = particles.get(tick);
            if (v == null) {
                v = new ArrayList<>();
                particles.put(tick, v);
            }
            v.add(Pair.of(pos, ParticleData.builder()
                    .data(data)
                    .particle(type)
                    .maxSpeed(speed)
                    .count(count)
                    .xDist((float) dist.x())
                    .yDist((float) dist.y())
                    .zDist((float) dist.z())
                    .alwaysShow(alwaysShow)
                    .build()));
        }
        Long2ObjectOpenHashMap<ParticleSource> sources = new Long2ObjectOpenHashMap<>();
        long finalMinimum = minimum;
        particles.forEach((k, l) -> {
            var s = new ParticleSource() {
                @Override
                public void doWrite(PacketBuilder out, double x, double y, double z) {
                    for (Pair<Vec3d, ParticleData> pair : l) {
                        var v = pair.getLeft();
                        out.append(pair.getRight(), x + v.x(), y + v.y(), z + v.z());
                    }
                }
            }.compute();
            sources.put(k - finalMinimum, s);
        });
        return new ParticleSpawnerData(max - minimum, sources);
    }

    private static class BySchemaParser {
        private String[] values;
        private final HashMap<String, Integer> schemaIndex = new HashMap<>();

        public void setSchema(String s) {
            String[] parts = s.split(",");
            this.schemaIndex.clear();
            for (int i = 0; i < parts.length; i++) {
                schemaIndex.put(parts[i], i);
            }
        }

        public void setValues(String s) {
            this.values = s.split(",");
        }

        public String get(String key) {
            Integer index = schemaIndex.get(key);
            return index != null ? values[index] : null;
        }

        public <T> T decode(String key, YamlCodec<T> codec) {
            return codec.decode(get(key)).getOrThrow();
        }
    }
}