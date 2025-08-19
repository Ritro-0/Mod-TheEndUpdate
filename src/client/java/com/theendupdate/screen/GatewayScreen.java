package com.theendupdate.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;

 

public class GatewayScreen extends HandledScreen<GatewayScreenHandler> {
	private static final Identifier TEXTURE = Identifier.of("theendupdate", "textures/gui/container/quantum_gateway.png");

	public GatewayScreen(GatewayScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 176;
		this.backgroundHeight = 166;
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int x = (this.width - this.backgroundWidth) / 2;
		int y = (this.height - this.backgroundHeight) / 2;

		// Draw the standalone PNG directly. Pass real texture size to avoid UV sampling artifacts.
		try {
			Object pipeline = resolveRenderPipeline(context);
			if (pipeline != null) {
				for (java.lang.reflect.Method m : context.getClass().getMethods()) {
					if (!m.getName().equals("drawTexture")) continue;
					Class<?>[] p = m.getParameterTypes();
					if (p.length >= 10 && p[0].getName().contains("RenderPipeline") && p[1] == Identifier.class) {
						Object[] args = new Object[p.length];
						args[0] = pipeline;
						args[1] = TEXTURE;
						args[2] = coerce(p[2], x);
						args[3] = coerce(p[3], y);
						args[4] = coerce(p[4], 0f);
						args[5] = coerce(p[5], 0f);
						args[6] = coerce(p[6], this.backgroundWidth);
						args[7] = coerce(p[7], this.backgroundHeight);
						args[8] = coerce(p[8], 256); // texture width
						args[9] = coerce(p[9], 256); // texture height
						for (int i = 10; i < p.length; i++) args[i] = zeroOf(p[i]);
						m.invoke(context, args);
						return;
					}
				}
			}
		} catch (Throwable ignored) {}

		// Fallback: try a no-pipeline overload (if present in these mappings)
		try {
			for (java.lang.reflect.Method m : context.getClass().getMethods()) {
				if (!m.getName().equals("drawTexture")) continue;
				Class<?>[] p = m.getParameterTypes();
				if (p.length >= 9 && p[0] == Identifier.class) {
					Object[] args = new Object[p.length];
					args[0] = TEXTURE;
					args[1] = coerce(p[1], x);
					args[2] = coerce(p[2], y);
					args[3] = coerce(p[3], 0f);
					args[4] = coerce(p[4], 0f);
					args[5] = coerce(p[5], this.backgroundWidth);
					args[6] = coerce(p[6], this.backgroundHeight);
					args[7] = coerce(p[7], 256);
					args[8] = coerce(p[8], 256);
					for (int i = 9; i < p.length; i++) args[i] = zeroOf(p[i]);
					m.invoke(context, args);
					return;
				}
			}
		} catch (Throwable ignored) {}

		// Last resort: gray fill so the UI isn't invisible
		context.fill(x, y, x + this.backgroundWidth, y + this.backgroundHeight, 0xFF8B8B8B);
	}

	private static Identifier tryGetAtlasId(Sprite sprite) {
		try {
			for (java.lang.reflect.Method m : sprite.getClass().getMethods()) {
				if (m.getParameterCount() == 0 && m.getReturnType() == Identifier.class) {
					String n = m.getName();
					if (n.toLowerCase().contains("atlas")) {
						m.setAccessible(true);
						Object v = m.invoke(sprite);
						if (v instanceof Identifier id) return id;
					}
				}
			}
		} catch (Throwable ignored) {}
		return null;
	}

	private static int tryCallInt(Object target, String name) {
		try {
			var m = target.getClass().getMethod(name);
			m.setAccessible(true);
			Object v = m.invoke(target);
			if (v instanceof Integer i) return i;
		} catch (Throwable ignored) {}
		return -1;
	}

	private static int[] tryGetTextureSize(Identifier id) {
		try {
			var mc = MinecraftClient.getInstance();
			var tex = mc.getTextureManager().getTexture(id);
			if (tex != null) {
				int w = -1, h = -1;
				try {
					var mw = tex.getClass().getMethod("getWidth");
					mw.setAccessible(true);
					Object vw = mw.invoke(tex);
					if (vw instanceof Integer iw) w = iw;
				} catch (Throwable ignored) {}
				try {
					var mh = tex.getClass().getMethod("getHeight");
					mh.setAccessible(true);
					Object vh = mh.invoke(tex);
					if (vh instanceof Integer ih) h = ih;
				} catch (Throwable ignored) {}
				return new int[] { w, h };
			}
		} catch (Throwable ignored) {}
		return new int[] { -1, -1 };
	}



	private static Object coerce(Class<?> type, int value) {
		if (type == float.class || type == Float.class) return (float) value;
		return value;
	}

	private static Object coerce(Class<?> type, float value) {
		if (type == float.class || type == Float.class) return value;
		return (int) value;
	}

	private static Object zeroOf(Class<?> type) {
		if (type == float.class || type == Float.class) return 0.0f;
		if (type == int.class || type == Integer.class) return 0;
		return null;
	}

	private static Object resolveRenderPipeline(DrawContext context) {
		try {
			Class<?> cls = context.getClass();
			while (cls != null) {
				for (java.lang.reflect.Field f : cls.getDeclaredFields()) {
					Class<?> ft = f.getType();
					if (ft != null && ft.getName() != null && ft.getName().contains("RenderPipeline")) {
						f.setAccessible(true);
						Object value = f.get(context);
						if (value != null) return value;
					}
				}
				cls = cls.getSuperclass();
			}
		} catch (Throwable ignored) {}
		try {
			Class<?> cls = context.getClass();
			while (cls != null) {
				for (java.lang.reflect.Field f : cls.getDeclaredFields()) {
					Class<?> ft = f.getType();
					String tn = ft != null ? ft.getName() : null;
					if (tn != null && (tn.endsWith("GuiRenderer") || tn.contains("gui.render.GuiRenderer"))) {
						f.setAccessible(true);
						Object guiRenderer = f.get(context);
						if (guiRenderer == null) continue;
						Class<?> gr = guiRenderer.getClass();
						for (java.lang.reflect.Field rf : gr.getDeclaredFields()) {
							Class<?> rft = rf.getType();
							if (rft != null && rft.getName() != null && rft.getName().contains("RenderPipeline")) {
								rf.setAccessible(true);
								Object value = rf.get(guiRenderer);
								if (value != null) return value;
							}
						}
						for (java.lang.reflect.Method rm : gr.getMethods()) {
							if (rm.getParameterCount() == 0) {
								Class<?> rt = rm.getReturnType();
								if (rt != null && rt.getName() != null && rt.getName().contains("RenderPipeline")) {
									rm.setAccessible(true);
									Object value = rm.invoke(guiRenderer);
									if (value != null) return value;
								}
							}
						}
					}
				}
				cls = cls.getSuperclass();
			}
		} catch (Throwable ignored) {}
		try {
			for (java.lang.reflect.Method m : context.getClass().getMethods()) {
				if (m.getParameterCount() == 0) {
					Class<?> rt = m.getReturnType();
					if (rt != null && rt.getName() != null && rt.getName().contains("RenderPipeline")) {
						m.setAccessible(true);
						Object value = m.invoke(context);
						if (value != null) return value;
					}
				}
			}
		} catch (Throwable ignored) {}
		try {
			Class<?> gr = Class.forName("net.minecraft.client.gui.render.GuiRenderer");
			for (java.lang.reflect.Field f : gr.getDeclaredFields()) {
				Class<?> ft = f.getType();
				if (ft != null && ft.getName() != null && ft.getName().contains("RenderPipeline")) {
					f.setAccessible(true);
					Object value = f.get(null);
					if (value != null) return value;
				}
			}
		} catch (Throwable ignored) {}
		return null;
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 4210752, false);
		context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 4210752, false);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}
}