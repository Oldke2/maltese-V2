package com.frida.maltese.client;

import com.frida.maltese.MalteseMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

/** Parte SOLO del cliente (la parte visual): conecta entidad -> modelo -> renderer. */
public class MalteseClient implements ClientModInitializer {

    public static final EntityModelLayer MODEL_LAYER =
            new EntityModelLayer(new Identifier(MalteseMod.MOD_ID, "black_maltese"), "main");

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(MODEL_LAYER, BlackMalteseModel::getTexturedModelData);
        EntityRendererRegistry.register(MalteseMod.BLACK_MALTESE, BlackMalteseRenderer::new);
    }
}
