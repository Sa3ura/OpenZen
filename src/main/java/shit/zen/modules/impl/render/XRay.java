package shit.zen.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import shit.zen.event.EventTarget;
import shit.zen.event.impl.MotionEvent;
import shit.zen.event.impl.RenderEvent;
import shit.zen.modules.Category;
import shit.zen.modules.Module;
import shit.zen.settings.impl.BooleanSetting;
import shit.zen.settings.impl.NumberSetting;
import shit.zen.utils.game.BlockUtil;
import shit.zen.utils.render.RenderUtil;

public class XRay extends Module {
    public static XRay INSTANCE;

    private final NumberSetting radius = new NumberSetting("Radius", 16.0, 6.0, 48.0, 1.0);
    private final BooleanSetting diamond = new BooleanSetting("Diamond", true);
    private final BooleanSetting emerald = new BooleanSetting("Emerald", true);
    private final BooleanSetting gold = new BooleanSetting("Gold", true);
    private final BooleanSetting iron = new BooleanSetting("Iron", true);
    private final BooleanSetting redstone = new BooleanSetting("Redstone", true);
    private final BooleanSetting lapis = new BooleanSetting("Lapis", true);
    private final BooleanSetting coal = new BooleanSetting("Coal", false);

    private final List<FoundOre> foundOres = new CopyOnWriteArrayList<>();

    public XRay() {
        super("XRay", Category.RENDER);
        INSTANCE = this;
    }

    @Override
    protected void onDisable() {
        this.foundOres.clear();
        super.onDisable();
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (!event.isPost() || mc.player == null || mc.level == null) {
            return;
        }

        int scanRadius = this.radius.getValue().intValue();
        BlockPos center = mc.player.blockPosition();
        this.foundOres.clear();

        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius; y <= scanRadius; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    Block block = BlockUtil.getBlock(pos);
                    if (block == null || !this.shouldShow(block)) {
                        continue;
                    }

                    AABB box = new AABB(pos);
                    float[] color = this.getColor(block);
                    this.foundOres.add(new FoundOre(box, color));
                }
            }
        }
    }

    @EventTarget
    public void onRender(RenderEvent event) {
        if (this.foundOres.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.poseStack();
        poseStack.pushPose();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionShader);

        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        for (FoundOre ore : this.foundOres) {
            RenderSystem.setShaderColor(ore.color[0], ore.color[1], ore.color[2], 0.28f);
            RenderUtil.drawBoxVerts(bufferBuilder, poseStack.last().pose(), ore.box);
        }

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();
    }

    private boolean shouldShow(Block block) {
        return this.diamond.getValue() && (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE)
                || this.emerald.getValue() && (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE)
                || this.gold.getValue() && (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE)
                || this.iron.getValue() && (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE)
                || this.redstone.getValue() && (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE)
                || this.lapis.getValue() && (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE)
                || this.coal.getValue() && (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE);
    }

    private float[] getColor(Block block) {
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return new float[]{0.2f, 0.9f, 1.0f};
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) return new float[]{0.2f, 1.0f, 0.45f};
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) return new float[]{1.0f, 0.86f, 0.15f};
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) return new float[]{0.9f, 0.72f, 0.56f};
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) return new float[]{1.0f, 0.2f, 0.2f};
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) return new float[]{0.2f, 0.45f, 1.0f};
        return new float[]{0.2f, 0.2f, 0.2f};
    }

    private record FoundOre(AABB box, float[] color) {
    }
}
