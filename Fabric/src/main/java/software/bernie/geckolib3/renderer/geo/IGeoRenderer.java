package software.bernie.geckolib3.renderer.geo;

import javax.annotation.Nonnull;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vector4f;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.geo.render.built.GeoQuad;
import software.bernie.geckolib3.geo.render.built.GeoVertex;
import software.bernie.geckolib3.model.provider.GeoModelProvider;
import software.bernie.geckolib3.util.EModelRenderCycle;
import software.bernie.geckolib3.util.IRenderCycle;
import software.bernie.geckolib3.util.RenderUtils;

public interface IGeoRenderer<T> {

	VertexConsumerProvider getCurrentRTB();

	GeoModelProvider getGeoModelProvider();

	Identifier getTextureLocation(T instance);

	default void render(GeoModel model, T animatable, float partialTicks, RenderLayer type, MatrixStack matrixStackIn,
			VertexConsumerProvider renderTypeBuffer, VertexConsumer vertexBuilder, int packedLightIn,
			int packedOverlayIn, float red, float green, float blue, float alpha) {
		this.setCurrentRTB(renderTypeBuffer);
		renderEarly(animatable, matrixStackIn, partialTicks, renderTypeBuffer, vertexBuilder, packedLightIn,
				packedOverlayIn, red, green, blue, alpha);

		if (renderTypeBuffer != null) {
			vertexBuilder = renderTypeBuffer.getBuffer(type);
		}
		renderLate(animatable, matrixStackIn, partialTicks, renderTypeBuffer, vertexBuilder, packedLightIn,
				packedOverlayIn, red, green, blue, alpha);
		// Render all top level bones
		for (GeoBone group : model.topLevelBones) {
			renderRecursively(group, matrixStackIn, vertexBuilder, packedLightIn, packedOverlayIn, red, green, blue,
					alpha);
		}
		// Since we rendered at least once at this point, let's set the cycle to
		// repeated
		this.setCurrentModelRenderCycle(EModelRenderCycle.REPEATED);
	}

	default void renderRecursively(GeoBone bone, MatrixStack stack, VertexConsumer bufferIn, int packedLightIn,
			int packedOverlayIn, float red, float green, float blue, float alpha) {
		stack.push();
		this.preparePositionRotationScale(bone, stack);
		this.renderCubesOfBone(bone, stack, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
		this.renderChildBones(bone, stack, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);

		stack.pop();
	}

	default void renderCubesOfBone(GeoBone bone, MatrixStack stack, VertexConsumer bufferIn, int packedLightIn,
			int packedOverlayIn, float red, float green, float blue, float alpha) {
		if (!bone.isHidden()) {
			for (GeoCube cube : bone.childCubes) {
				stack.push();
				if (!bone.cubesAreHidden()) {
					renderCube(cube, stack, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
				}
				stack.pop();
			}
		}
	}

	default void renderChildBones(GeoBone bone, MatrixStack stack, VertexConsumer bufferIn, int packedLightIn,
			int packedOverlayIn, float red, float green, float blue, float alpha) {
		if (!bone.childBonesAreHiddenToo()) {
			for (GeoBone childBone : bone.childBones) {
				renderRecursively(childBone, stack, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
			}
		}
	}

	default void preparePositionRotationScale(GeoBone bone, MatrixStack stack) {
		RenderUtils.translate(bone, stack);
		RenderUtils.moveToPivot(bone, stack);
		RenderUtils.rotate(bone, stack);
		RenderUtils.scale(bone, stack);
		RenderUtils.moveBackFromPivot(bone, stack);
	}

	default void renderCube(GeoCube cube, MatrixStack stack, VertexConsumer bufferIn, int packedLightIn,
			int packedOverlayIn, float red, float green, float blue, float alpha) {
		RenderUtils.moveToPivot(cube, stack);
		RenderUtils.rotate(cube, stack);
		RenderUtils.moveBackFromPivot(cube, stack);
		Matrix3f matrix3f = stack.peek().getNormal();
		Matrix4f matrix4f = stack.peek().getModel();

		for (GeoQuad quad : cube.quads) {
			if (quad == null) {
				continue;
			}
			Vec3f normal = quad.normal.copy();
			normal.transform(matrix3f);

			if ((cube.size.getY() == 0 || cube.size.getZ() == 0) && normal.getX() < 0) {
				normal.multiplyComponentwise(-1, 1, 1);
			}
			if ((cube.size.getX() == 0 || cube.size.getZ() == 0) && normal.getY() < 0) {
				normal.multiplyComponentwise(1, -1, 1);
			}
			if ((cube.size.getX() == 0 || cube.size.getY() == 0) && normal.getZ() < 0) {
				normal.multiplyComponentwise(1, 1, -1);
			}

			this.createVerticesOfQuad(quad, matrix4f, normal, bufferIn, packedLightIn, packedOverlayIn, red, green,
					blue, alpha);

		}
	}

	default void createVerticesOfQuad(GeoQuad quad, Matrix4f matrix4f, Vec3f normal, VertexConsumer bufferIn,
			int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
		for (GeoVertex vertex : quad.vertices) {
			Vector4f vector4f = new Vector4f(vertex.position.getX(), vertex.position.getY(), vertex.position.getZ(),
					1.0F);
			vector4f.transform(matrix4f);
			bufferIn.vertex(vector4f.getX(), vector4f.getY(), vector4f.getZ(), red, green, blue, alpha, vertex.textureU,
					vertex.textureV, packedOverlayIn, packedLightIn, normal.getX(), normal.getY(), normal.getZ());
		}
	}

	default void renderEarly(T animatable, MatrixStack stackIn, float partialTicks,
			VertexConsumerProvider renderTypeBuffer, VertexConsumer vertexBuilder, int packedLightIn,
			int packedOverlayIn, float red, float green, float blue, float alpha) {
		if (this.getCurrentModelRenderCycle() == EModelRenderCycle.INITIAL /* Pre-Layers */) {
			float width = this.getWidthScale(animatable);
			float height = this.getHeightScale(animatable);
			stackIn.scale(width, height, width);
		}
	}

	default void renderLate(T animatable, MatrixStack stackIn, float partialTicks,
			VertexConsumerProvider renderTypeBuffer, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn,
			float red, float green, float blue, float alpha) {
	}

	default RenderLayer getRenderType(T animatable, float partialTicks, MatrixStack stack,
			VertexConsumerProvider renderTypeBuffer, VertexConsumer vertexBuilder, int packedLightIn,
			Identifier textureLocation) {
		return RenderLayer.getEntityCutout(textureLocation);
	}

	default Color getRenderColor(T animatable, float partialTicks, MatrixStack stack,
			VertexConsumerProvider renderTypeBuffer, VertexConsumer vertexBuilder, int packedLightIn) {
		return Color.ofRGBA(255, 255, 255, 255);
	}

	default Integer getUniqueID(T animatable) {
		return animatable.hashCode();
	}

	public default void setCurrentModelRenderCycle(IRenderCycle cycle) {

	}

	@Nonnull
	public default IRenderCycle getCurrentModelRenderCycle() {
		return EModelRenderCycle.INITIAL;
	}

	public default void setCurrentRTB(VertexConsumerProvider rtb) {

	}

	public default float getWidthScale(T animatable2) {
		return 1F;
	}

	public default float getHeightScale(T entity) {
		return 1F;
	}
}
