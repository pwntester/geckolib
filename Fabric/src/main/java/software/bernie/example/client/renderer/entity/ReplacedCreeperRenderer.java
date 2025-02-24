package software.bernie.example.client.renderer.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.math.MathHelper;
import software.bernie.example.client.model.entity.ReplacedCreeperModel;
import software.bernie.example.entity.ReplacedCreeperEntity;
import software.bernie.geckolib3.renderers.geo.GeoReplacedEntityRenderer;

public class ReplacedCreeperRenderer extends GeoReplacedEntityRenderer<ReplacedCreeperEntity> {

	public ReplacedCreeperRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new ReplacedCreeperModel(), new ReplacedCreeperEntity());
		GeoReplacedEntityRenderer.registerReplacedEntity(ReplacedCreeperEntity.class, this);
	}

	@Override
	protected void preRenderCallback(LivingEntity entity, MatrixStack poseStack,
			float partialTick) {
		CreeperEntity creeper = (CreeperEntity) entity;
		float f = creeper.getClientFuseTime(partialTick);
		float f1 = 1.0F + MathHelper.sin(f * 100.0F) * f * 0.01F;
		f = MathHelper.clamp(f, 0.0F, 1.0F);
		f = f * f;
		f = f * f;
		float f2 = (1.0F + f * 0.4F) * f1;
		float f3 = (1.0F + f * 0.1F) / f1;
		poseStack.scale(f2, f3, f2);
	}

	@Override
	protected float getOverlayProgress(LivingEntity entity, float partialTick) {
		CreeperEntity creeper = (CreeperEntity) entity;
		float f = creeper.getClientFuseTime(partialTick);
		return (int) (f * 10.0F) % 2 == 0 ? 0.0F : MathHelper.clamp(f, 0.5F, 1.0F);
	}
}