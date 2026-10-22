package com.frida.maltese.client;

import com.frida.maltese.MalteseMod;
import com.frida.maltese.entity.BlackMalteseEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/** El "pintor": une el modelo con la textura y dibuja la entidad. */
public class BlackMalteseRenderer extends MobEntityRenderer<BlackMalteseEntity, BlackMalteseModel> {

    private static final Identifier TEXTURE =
            new Identifier(MalteseMod.MOD_ID, "textures/entity/black_maltese.png");

    public BlackMalteseRenderer(EntityRendererFactory.Context ctx) {
        // 0.3f = tamano de la sombra en el suelo
        super(ctx, new BlackMalteseModel(ctx.getPart(MalteseClient.MODEL_LAYER)), 0.3f);
    }

    @Override
    public Identifier getTexture(BlackMalteseEntity entity) {
        return TEXTURE;
    }
}
