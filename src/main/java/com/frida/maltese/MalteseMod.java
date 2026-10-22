package com.frida.maltese;

import com.frida.maltese.entity.BlackMalteseEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.BiomeKeys;

import java.util.List;

/**
 * Clase principal del mod. Fabric la ejecuta al arrancar el juego.
 * Aqui REGISTRAMOS todo: la entidad, el huevo, el spawn y los comandos de chat.
 */
public class MalteseMod implements ModInitializer {

    public static final String MOD_ID = "maltesemod";

    // 1) Tipo de entidad: dice a Minecraft "existe un bicho llamado maltesemod:black_maltese"
    //    Tamano de la hitbox: 0.5 ancho x 0.6 alto (pequenita).
    public static final EntityType<BlackMalteseEntity> BLACK_MALTESE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MOD_ID, "black_maltese"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, BlackMalteseEntity::new)
                    .dimensions(EntityDimensions.fixed(0.5f, 0.6f))
                    .trackRangeBlocks(8)
                    .build());

    // 2) Huevo de spawn llamado "Black Maltese Puppy" (el nombre sale del archivo lang).
    //    Colores: negro (cuerpo) y blanco (manchitas del huevo).
    public static final Item BLACK_MALTESE_PUPPY_EGG = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "black_maltese_puppy"),
            new SpawnEggItem(BLACK_MALTESE, 0x111111, 0xF2F2F2, new FabricItemSettings()));

    @Override
    public void onInitialize() {
        // Atributos: vida, velocidad, etc.
        FabricDefaultAttributeRegistry.register(BLACK_MALTESE, BlackMalteseEntity.createAttributes());

        // Donde puede aparecer: en el suelo, y sobre bloques de césped (regla de animales).
        SpawnRestriction.register(BLACK_MALTESE, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn);

        // Spawn natural RARO (peso 3; las ovejas tienen 12) en llanuras y bosques, grupos de 1 a 2.
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS,
                        BiomeKeys.FOREST, BiomeKeys.FLOWER_FOREST, BiomeKeys.BIRCH_FOREST,
                        BiomeKeys.DARK_FOREST),
                SpawnGroup.CREATURE, BLACK_MALTESE, 3, 1, 2);

        // El huevo aparece en la pestana creativa de huevos de spawn.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS)
                .register(entries -> entries.add(BLACK_MALTESE_PUPPY_EGG));

        // Comandos por chat: escribir "sit" sienta a tus Maltesas, "up" las levanta.
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String text = message.getSignedContent().trim().toLowerCase();
            boolean sit = text.equals("sit") || text.equals("sientate") || text.equals("si\u00e9ntate");
            boolean up = text.equals("up") || text.equals("levanta") || text.equals("lev\u00e1ntate");
            if (!sit && !up) return;

            sender.getServer().execute(() -> {
                List<BlackMalteseEntity> dogs = sender.getServerWorld().getEntitiesByClass(
                        BlackMalteseEntity.class,
                        sender.getBoundingBox().expand(24),
                        dog -> dog.isTamed() && dog.isOwner(sender));
                for (BlackMalteseEntity dog : dogs) {
                    dog.commandSit(sit);
                }
            });
        });
    }
}
