package com.unholymc.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.unholymc.UnholyMC;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

import java.util.Optional;

/** Item data components. The model-facing state is mirrored into minecraft:custom_model_data (docs/ASSETS.md §2). */
public final class ModComponents {
    /** Flashlight: on/off and the inserted core item. */
    public record FlashlightData(boolean on, Optional<Holder<Item>> core) {
        public static final FlashlightData EMPTY = new FlashlightData(false, Optional.empty());
        public static final Codec<FlashlightData> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("on", false).forGetter(FlashlightData::on),
                Item.CODEC.optionalFieldOf("core").forGetter(FlashlightData::core)
        ).apply(i, FlashlightData::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, FlashlightData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, FlashlightData::on,
                ByteBufCodecs.optional(Item.STREAM_CODEC), FlashlightData::core,
                FlashlightData::new);
    }

    /** Thermos: one fluid at a time (DECISIONS #26). */
    public record ThermosContents(String fluid, int portions) {
        public static final ThermosContents EMPTY = new ThermosContents("", 0);
        public static final Codec<ThermosContents> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("fluid", "").forGetter(ThermosContents::fluid),
                Codec.INT.optionalFieldOf("portions", 0).forGetter(ThermosContents::portions)
        ).apply(i, ThermosContents::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ThermosContents> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ThermosContents::fluid,
                ByteBufCodecs.VAR_INT, ThermosContents::portions,
                ThermosContents::new);

        public boolean isEmpty() {
            return portions <= 0 || fluid.isEmpty();
        }
    }

    public static final DataComponentType<FlashlightData> FLASHLIGHT = register("flashlight",
            DataComponentType.<FlashlightData>builder().persistent(FlashlightData.CODEC).networkSynchronized(FlashlightData.STREAM_CODEC).build());
    public static final DataComponentType<ThermosContents> THERMOS = register("thermos",
            DataComponentType.<ThermosContents>builder().persistent(ThermosContents.CODEC).networkSynchronized(ThermosContents.STREAM_CODEC).build());
    /** Unholy Sword at 0 durability: discharged instead of broken (DECISIONS #25). */
    public static final DataComponentType<Boolean> DISCHARGED = register("discharged",
            DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    private ModComponents() {}

    private static <T> DataComponentType<T> register(String name, DataComponentType<T> type) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, UnholyMC.id(name), type);
    }

    public static void init() {}
}
