package ru.zorotexx.recordReader.utils;

import dev.by1337.yaml.codec.InlineYamlCodecBuilder;
import dev.by1337.yaml.codec.YamlCodec;

public record Vec3d(double x, double y, double z) {
    public static final YamlCodec<Vec3d> CODEC = InlineYamlCodecBuilder.inline(
            ";",
            "<x>;<y>;<z>",
            Vec3d::new,
            YamlCodec.DOUBLE.withGetter(Vec3d::x),
            YamlCodec.DOUBLE.withGetter(Vec3d::y),
            YamlCodec.DOUBLE.withGetter(Vec3d::z)
    );

    public Vec3d and(Vec3d o){
        return  new Vec3d(
                x + o.x,
                y + o.y,
                z + o.z
        );
    }

    public Vec3d sub(Vec3d o){
        return  new Vec3d(
                x - o.x,
                y - o.y,
                z - o.z
        );
    }
}