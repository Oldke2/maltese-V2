package com.frida.maltese.client;

import com.frida.maltese.entity.BlackMalteseEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

/**
 * MODELO 3D + ANIMACIONES de la Maltese.
 * Textura de 64x64. En el modelo, Y crece hacia ABAJO y el suelo esta en y = 24.
 * Todas las medidas estan en "pixeles" (16 pixeles = 1 bloque).
 */
public class BlackMalteseModel extends EntityModel<BlackMalteseEntity> {

    private static final float RAD = (float) (Math.PI / 180.0);

    private final ModelPart root, body, head, tongue, earLeft, earRight, tail;
    private final ModelPart frontLeft, frontRight, backLeft, backRight;

    public BlackMalteseModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.tongue = head.getChild("tongue");
        this.earLeft = head.getChild("ear_left");
        this.earRight = head.getChild("ear_right");
        this.tail = root.getChild("tail");
        this.frontLeft = root.getChild("front_left_leg");
        this.frontRight = root.getChild("front_right_leg");
        this.backLeft = root.getChild("back_left_leg");
        this.backRight = root.getChild("back_right_leg");
    }

    // ------------------------------------------------------------------
    //  1) GEOMETRIA: cada .uv(u, v) indica donde empieza esa pieza en la textura
    // ------------------------------------------------------------------
    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        // Cuerpo 6x5x9 + "faldon" de pelo largo a los lados
        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create().uv(0, 12).cuboid(-3, 0, -4.5f, 6, 5, 9),
                ModelTransform.pivot(0, 15, 0));
        body.addChild("skirt",
                ModelPartBuilder.create().uv(0, 42).cuboid(-4, 2, -5, 8, 3, 10),
                ModelTransform.NONE);

        // Cabeza 6x5x5 (cara con ojos grandes) + hocico corto + mono (top-knot)
        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create().uv(0, 0).cuboid(-3, -4, -5, 6, 5, 5),
                ModelTransform.pivot(0, 16, -4.5f));
        head.addChild("snout",
                ModelPartBuilder.create().uv(24, 0).cuboid(-2, -1, -7, 4, 3, 2),
                ModelTransform.NONE);
        head.addChild("topknot",
                ModelPartBuilder.create().uv(40, 12).cuboid(-1.5f, -6, -3.5f, 3, 2, 3),
                ModelTransform.NONE);
        head.addChild("tongue",
                ModelPartBuilder.create().uv(44, 20).cuboid(-1, 0, -1, 2, 1, 2),
                ModelTransform.pivot(0, 1.6f, -6.5f));

        // Orejas caidas (largas, 1x5x3), un poco abiertas hacia afuera
        head.addChild("ear_left",
                ModelPartBuilder.create().uv(36, 0).cuboid(0, 0, -1.5f, 1, 5, 3),
                ModelTransform.of(3, -3.5f, -2.5f, 0, 0, -0.15f));
        head.addChild("ear_right",
                ModelPartBuilder.create().uv(36, 0).cuboid(-1, 0, -1.5f, 1, 5, 3),
                ModelTransform.of(-3, -3.5f, -2.5f, 0, 0, 0.15f));

        // Patitas 2x4x2
        ModelPartBuilder leg = ModelPartBuilder.create().uv(32, 12).cuboid(-1, 0, -1, 2, 4, 2);
        root.addChild("front_left_leg", leg, ModelTransform.pivot(2, 20, -3));
        root.addChild("front_right_leg", leg, ModelTransform.pivot(-2, 20, -3));
        root.addChild("back_left_leg", leg, ModelTransform.pivot(2, 20, 3));
        root.addChild("back_right_leg", leg, ModelTransform.pivot(-2, 20, 3));

        // Cola
        root.addChild("tail",
                ModelPartBuilder.create().uv(32, 20).cuboid(-1, -1, 0, 2, 2, 4),
                ModelTransform.pivot(0, 15.5f, 4.5f));

        return TexturedModelData.of(data, 64, 64);
    }

    // ------------------------------------------------------------------
    //  2) ANIMACION: se llama en cada fotograma
    //     limbAngle    = "fase" del paso (crece al caminar)
    //     limbDistance = intensidad del paso (0 quieta ... 1 a toda velocidad)
    //     age          = tiempo continuo en ticks (para animaciones en bucle)
    // ------------------------------------------------------------------
    @Override
    public void setAngles(BlackMalteseEntity e, float limbAngle, float limbDistance,
                          float age, float headYaw, float headPitch) {

        // --- A. Postura base (se reinicia cada fotograma) ---
        root.setPivot(0, 0, 0);
        root.setAngles(0, 0, 0);
        body.setPivot(0, 15, 0);
        body.setAngles(0, 0, 0);
        head.setPivot(0, 16, -4.5f);
        head.setAngles(headPitch * RAD, headYaw * RAD, 0);
        tongue.visible = false;
        tongue.setPivot(0, 1.6f, -6.5f);
        tongue.setAngles(0, 0, 0);
        frontLeft.setPivot(2, 20, -3);
        frontRight.setPivot(-2, 20, -3);
        backLeft.setPivot(2, 20, 3);
        backRight.setPivot(-2, 20, 3);
        tail.setPivot(0, 15.5f, 4.5f);
        earLeft.setAngles(0, 0, -0.15f);
        earRight.setAngles(0, 0, 0.15f);

        // --- B. CAMINAR (trote suave) y CORRER (galope) ---
        double dx = e.getX() - e.prevX;
        double dz = e.getZ() - e.prevZ;
        float speed = (float) Math.sqrt(dx * dx + dz * dz);
        float run = MathHelper.clamp((speed - 0.27f) / 0.08f, 0f, 1f); // 0 = camina, 1 = corre

        float s = limbAngle * 0.6662f;
        float walkA = MathHelper.cos(s) * 1.2f * limbDistance;
        float walkB = MathHelper.cos(s + MathHelper.PI) * 1.2f * limbDistance;
        float g = limbAngle * 0.6662f * 0.8f;
        float gallopFront = MathHelper.cos(g) * 1.5f * limbDistance;
        float gallopBack = MathHelper.cos(g + 1.0f) * 1.5f * limbDistance;

        frontLeft.pitch = MathHelper.lerp(run, walkA, gallopFront);
        backRight.pitch = MathHelper.lerp(run, walkA, gallopBack);
        frontRight.pitch = MathHelper.lerp(run, walkB, gallopFront);
        backLeft.pitch = MathHelper.lerp(run, walkB, gallopBack);

        body.pitch = run * MathHelper.sin(g) * 0.12f * limbDistance;      // el lomo ondula al galopar
        root.pivotY = -run * Math.abs(MathHelper.sin(g)) * 1.2f * limbDistance; // saltitos al correr
        earLeft.roll -= MathHelper.sin(s) * 0.12f * limbDistance;          // orejas que rebotan
        earRight.roll += MathHelper.sin(s) * 0.12f * limbDistance;

        // --- C. COLA FELIZ: mueve la cola rapido si esta domesticada y su dueno esta cerca ---
        LivingEntity owner = e.isTamed() ? e.getOwner() : null;
        boolean happy = owner != null && e.squaredDistanceTo(owner) < 100;
        tail.pitch = 0.9f - run * 0.5f;
        tail.yaw = MathHelper.sin(age * (happy ? 0.9f : 0.15f)) * (happy ? 0.7f : 0.15f);

        // --- D. ANIMACIONES ESPECIALES ---
        int action = e.getAction();
        float w = 0f; // "peso" 0..1: sube suave al empezar y baja suave al terminar
        if (action != BlackMalteseEntity.ACTION_NONE) {
            float p = 1f - e.getActionTicks() / (float) BlackMalteseEntity.getActionDuration(action);
            w = MathHelper.clamp(MathHelper.sin(p * MathHelper.PI) * 2.5f, 0f, 1f);
        }

        if (action == BlackMalteseEntity.ACTION_DANCE) {
            poseDance(age, w);
        } else {
            boolean sitting = e.isInSittingPose();
            if (sitting) {
                poseSit();
            }
            if (action == BlackMalteseEntity.ACTION_LICK) {
                poseLick(age, w, sitting);
            } else if (action == BlackMalteseEntity.ACTION_STRETCH && !sitting) {
                poseStretch(w);
            }
        }
    }

    /** SENTADA: trasero abajo, pecho arriba, patas traseras dobladas hacia adelante. */
    private void poseSit() {
        body.setPivot(0, 17.3f, 0.5f);
        body.pitch = -0.7f;
        head.setPivot(0, 15.2f, -3.0f);
        frontLeft.setPivot(2, 20, -3.5f);
        frontRight.setPivot(-2, 20, -3.5f);
        frontLeft.pitch = 0;
        frontRight.pitch = 0;
        backLeft.setPivot(2.4f, 22.5f, 1.5f);
        backRight.setPivot(-2.4f, 22.5f, 1.5f);
        backLeft.pitch = -1.3f;
        backRight.pitch = -1.3f;
        tail.setPivot(0, 20.4f, 3.8f);
        tail.pitch = 0.4f;
        root.pivotY = 0;
    }

    /** LAMERSE: baja la cabeza, saca la lengua y (si esta de pie) levanta una patita. */
    private void poseLick(float age, float w, boolean sitting) {
        head.pitch += 0.6f * w;
        tongue.visible = w > 0.2f;
        tongue.pivotY = 1.6f + MathHelper.sin(age * 1.2f) * 0.3f;
        tongue.pitch = MathHelper.sin(age * 1.2f) * 0.25f;
        if (!sitting) {
            head.yaw -= 0.4f * w;
            frontLeft.pitch = -1.2f * w;
        }
    }

    /** ESTIRARSE: "reverencia de juego" - pecho abajo, trasero y cola arriba. */
    private void poseStretch(float w) {
        body.pitch = 0.45f * w;
        head.setPivot(0, 16 + 2.0f * w, -4.5f + 0.4f * w);
        frontLeft.pitch = -0.35f * w;
        frontRight.pitch = -0.35f * w;
        tail.setPivot(0, 15.5f - 1.9f * w, 4.5f - 0.45f * w);
        tail.pitch = 0.9f + 0.6f * w;
    }

    /** BAILE DEL HUESO: se levanta sobre las patas traseras, saltitos, giros y cola a toda velocidad. */
    private void poseDance(float age, float w) {
        float beat = age * 0.45f;
        float hop = Math.abs(MathHelper.sin(beat));

        root.pivotY = -hop * 2.5f * w;                         // saltitos
        root.yaw = MathHelper.sin(beat * 0.5f) * 0.5f * w;     // se balancea de lado a lado

        body.setPivot(0, 15 + 1.5f * w, 0.5f * w);
        body.pitch = -0.55f * w;                                // se pone de pie
        head.setPivot(0, 16 - 0.8f * w, -4.5f + 0.7f * w);
        head.roll = MathHelper.sin(beat) * 0.25f * w;           // ladea la cabeza

        // patitas delanteras arriba "aplaudiendo"
        frontLeft.setPivot(2, MathHelper.lerp(w, 20, 18.5f), MathHelper.lerp(w, -3, -3.3f));
        frontRight.setPivot(-2, MathHelper.lerp(w, 20, 18.5f), MathHelper.lerp(w, -3, -3.3f));
        frontLeft.pitch = -2.0f * w + MathHelper.sin(age * 0.9f) * 0.6f * w;
        frontRight.pitch = -2.0f * w - MathHelper.sin(age * 0.9f) * 0.6f * w;
        // pasitos de baile con las traseras
        backLeft.pitch = MathHelper.sin(age * 0.9f) * 0.3f * w;
        backRight.pitch = -MathHelper.sin(age * 0.9f) * 0.3f * w;

        // cola muy rapida
        tail.pitch = 1.1f;
        tail.yaw = MathHelper.sin(age * 1.7f) * 0.9f;

        // orejas que vuelan con cada salto
        earLeft.roll = -0.15f - 0.5f * hop * w;
        earRight.roll = 0.15f + 0.5f * hop * w;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
