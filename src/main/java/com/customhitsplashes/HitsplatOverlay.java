package com.customhitsplashes;

import com.customhitsplashes.CustomHitsplashesConfig.Theme;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Value;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class HitsplatOverlay extends Overlay
{
	static final int SLOTS = 4;
	private static final int[][] SLOT_OFFSETS = {{0, 0}, {0, -20}, {-15, -10}, {15, -10}};
	private static final int SHOW_EXTRA_CYCLES = 20;
	private static final int CALM_EXIT = 8;
	private static final double MARGIN = 1.5;
	private static final Map<String, int[][]> PARTICLES = Map.of(
		"dot", new int[][]{{0, 0}, {1, 0}, {0, 1}, {1, 1}},
		"star", new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}},
		"heart", new int[][]{{-1, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}, {2, 1}, {-1, 2}, {0, 2}, {1, 2}, {0, 3}, {-2, 0}, {2, 0}},
		"bubble", new int[][]{{-1, -1}, {0, -1}, {1, -1}, {-1, 1}, {0, 1}, {1, 1}, {-1, 0}, {1, 0}}
	);

	@RequiredArgsConstructor
	private static final class Splat
	{
		private final int type;
		private final String text;
		private final int start;
		private final int end;
		private final boolean others;
		private final boolean plain;
		private boolean laidOut;
		private BufferedImage sprite;
		private int spriteX;
		private int spriteY;
		private int textX;
		private int textY;
		private double numberX;
		private double numberY;
		private ThemeArt.Art art;
	}

	private final Client client;
	private final ThemeArt art;
	private final Map<Actor, Splat[]> splats = new HashMap<>();

	@Setter
	private Theme theme;

	@Inject
	HitsplatOverlay(Client client, ThemeArt art)
	{
		this.client = client;
		this.art = art;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.UNDER_WIDGETS);
	}

	void add(Actor actor, int type, int amount, int endCycle, boolean others, boolean plain)
	{
		Splat[] slots = splats.computeIfAbsent(actor, a -> new Splat[SLOTS]);
		int[] ends = new int[SLOTS];
		for (int i = 0; i < SLOTS; i++)
		{
			ends[i] = slots[i] == null ? Integer.MIN_VALUE : slots[i].end;
		}
		int start = endCycle - HitsplatDefs.cycles(type) - SHOW_EXTRA_CYCLES;
		slots[pickSlot(ends, client.getGameCycle())] = new Splat(type, String.valueOf(Math.max(0, amount)), start, endCycle, others, plain);
	}

	static int pickSlot(int[] ends, int now)
	{
		boolean full = true;
		int cursor = 0;
		for (int i = 0; i < ends.length; i++)
		{
			if (ends[i] > now)
			{
				cursor = (i + 1) % ends.length;
			}
			else
			{
				full = false;
			}
		}

		if (full)
		{
			int oldest = 0;
			for (int i = 1; i < ends.length; i++)
			{
				if (ends[i] < ends[oldest])
				{
					oldest = i;
				}
			}
			return oldest;
		}

		for (int k = 0; k < ends.length; k++)
		{
			int i = (cursor + k) % ends.length;
			if (ends[i] <= now)
			{
				return i;
			}
		}
		return 0;
	}

	void restyle(Theme theme)
	{
		this.theme = theme;
		splats.values().forEach(slots ->
		{
			for (Splat s : slots)
			{
				if (s != null)
				{
					s.laidOut = false;
				}
			}
		});
	}

	void remove(Actor actor)
	{
		splats.remove(actor);
	}

	void clear()
	{
		splats.clear();
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (theme == null || splats.isEmpty())
		{
			return null;
		}

		int now = client.getGameCycle();
		Composite composite = g.getComposite();
		Object interpolation = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

		for (Iterator<Map.Entry<Actor, Splat[]>> it = splats.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<Actor, Splat[]> entry = it.next();
			Splat[] slots = entry.getValue();
			Point anchor = null;
			boolean alive = false;
			for (int i = 0; i < SLOTS; i++)
			{
				Splat s = slots[i];
				if (s == null)
				{
					continue;
				}
				if (now >= s.end + exitCycles(s))
				{
					slots[i] = null;
					continue;
				}
				alive = true;
				if (now < s.start || !art.isLoaded(HitsplatDefs.sprite(s.type)))
				{
					continue;
				}
				if (anchor == null)
				{
					anchor = anchor(entry.getKey());
					if (anchor == null)
					{
						break;
					}
				}
				draw(g, s, now, anchor.getX() + SLOT_OFFSETS[i][0], anchor.getY() + SLOT_OFFSETS[i][1]);
			}
			if (!alive)
			{
				it.remove();
			}
		}

		g.setComposite(composite);
		if (interpolation != null)
		{
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
		}
		return null;
	}

	private Point anchor(Actor actor)
	{
		LocalPoint lp = actor.getLocalLocation();
		if (lp == null)
		{
			return null;
		}
		WorldView wv = client.getWorldView(lp.getWorldView());
		if (wv == null)
		{
			return null;
		}
		int ground = Perspective.getFootprintTileHeight(client, lp, wv.getPlane(), actor.getFootprintSize());
		int height = (actor.getLogicalHeight() + actor.getAnimationHeightOffset()) / 2;
		return Perspective.localToCanvas(client, lp.getWorldView(), lp.getX(), lp.getY(), ground - height);
	}

	private boolean vanilla(Splat s)
	{
		return s.plain || theme == Theme.CLASSIC;
	}

	private int exitCycles(Splat s)
	{
		return vanilla(s) ? 0 : s.others ? CALM_EXIT : theme.getExitCycles();
	}

	private void layout(Splat s)
	{
		s.sprite = art.sprite(s.type);
		int w = s.sprite.getWidth(), h = s.sprite.getHeight();
		int[] text = art.gameText(s.text);
		s.spriteX = -w / 2 + 1;
		s.spriteY = -h / 2;
		s.textX = s.spriteX + (w - text[0]) / 2;
		s.textY = s.spriteY + 15 + HitsplatDefs.textY(s.type);
		s.numberX = s.textX + (text[1] + text[3] + 1) / 2.0;
		s.numberY = s.textY + (text[2] + text[4] + 1) / 2.0;
		if (!vanilla(s))
		{
			int[] ours = art.numberSize(s.text, s.others);
			double hx = Math.max((text[3] - text[1] + 1) / 2.0, ours[0] / 2.0) + MARGIN;
			double hy = Math.max((text[4] - text[2] + 1) / 2.0, ours[1] / 2.0) + MARGIN;
			s.art = art.art(s.type, theme, s.others, hx, hy);
		}
		s.laidOut = true;
	}

	private void draw(Graphics2D g, Splat s, int now, int x, int y)
	{
		if (!s.laidOut)
		{
			layout(s);
		}
		if (vanilla(s))
		{
			drawVanilla(g, s, x, y);
			return;
		}

		ThemeArt.Art a = s.art;
		double cx = x + s.numberX, cy = y + s.numberY;
		boolean max = HitsplatDefs.isMax(s.type) && !s.others;
		int age = now - s.start, exit = now - s.end;
		Pose p = pose(theme, s.others, age, exit, max);
		double py = cy + a.pivotY;
		double dy = p.dy + (p.squish ? (1 - p.sy) * a.bottom : 0);

		BufferedImage image = a.image;
		if (a.glint != null && !s.others)
		{
			int sweep = max ? age % 30 : age;
			if (sweep < a.glint.length)
			{
				image = a.glint[sweep];
			}
		}

		if (p.popped < 0)
		{
			AffineTransform old = g.getTransform();
			g.translate(cx, py + dy);
			if (p.rot != 0)
			{
				g.rotate(p.rot);
			}
			g.scale(p.sx, p.sy);
			g.translate(a.drawX, a.drawY - a.pivotY);
			g.setComposite(alpha((float) p.alpha));
			g.drawImage(image, 0, 0, null);
			g.setTransform(old);
		}

		Color accent = new Color(theme.getAccent());
		if (p.run >= 0)
		{
			g.setComposite(alpha((float) p.alpha));
			g.setColor(new Color(a.dripColor[0], a.dripColor[1], a.dripColor[2]));
			int left = (int) Math.round(cx + a.drawX), top = (int) Math.round(cy + a.drawY);
			for (int[] d : a.drips)
			{
				g.fillRect(left + d[0], top + d[1], 1, (int) (p.run * 7));
			}
		}
		if (p.popped >= 0)
		{
			int[] pastel = ThemeArt.tone(a.vivid, .7f, .8f);
			Color bits = new Color(pastel[0], pastel[1], pastel[2]);
			for (int k = 0; k < 8; k++)
			{
				double angle = k * Math.PI / 4 + .2, r = a.size * (.5 + 1.1 * easeOut(p.popped));
				particle(g, k % 2 == 1 ? "dot" : "star", cx + Math.cos(angle) * r, py + Math.sin(angle) * r, k % 3 == 0 ? Color.WHITE : bits, 1 - p.popped);
			}
		}
		if (p.drift >= 0)
		{
			for (double[] star : new double[][]{{-.5, -.2}, {.4, -.4}, {.1, .3}})
			{
				particle(g, "star", cx + star[0] * a.size, py + star[1] * a.size - 12 * p.drift, accent, 1 - p.drift);
			}
		}
		if (!s.others && theme == Theme.STARDUST && age < 8)
		{
			for (double[] star : new double[][]{{.95, -.85}, {-1, .7}})
			{
				particle(g, "star", cx + star[0] * a.size, py + star[1] * a.size, accent, 1 - age / 8.0);
			}
		}
		if (max && age < 16)
		{
			double t = age / 16.0;
			for (int k = 0; k < 8; k++)
			{
				double angle = k * Math.PI / 4 + .4, r = a.size + 2 + 10 * easeOut(t);
				particle(g, theme.getFx(), cx + Math.cos(angle) * r * 1.1, py + Math.sin(angle) * r, accent, 1 - t);
			}
		}

		if (!s.text.isEmpty())
		{
			double scale = exit < 0 || p.squish ? Math.min(p.sx, p.sy) : 1;
			double numberAlpha = p.popped >= 0 ? 1 - p.popped * 2.5 : p.alpha;
			drawNumber(g, s, scale, (float) numberAlpha, cx, cy + dy);
		}
	}

	private static void drawVanilla(Graphics2D g, Splat s, int x, int y)
	{
		g.setComposite(AlphaComposite.SrcOver);
		g.drawImage(s.sprite, x + s.spriteX, y + s.spriteY, null);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g.setFont(FontManager.getRunescapeSmallFont());
		g.setColor(Color.BLACK);
		g.drawString(s.text, x + s.textX + 1, y + s.textY + 1);
		g.setColor(Color.WHITE);
		g.drawString(s.text, x + s.textX, y + s.textY);
	}

	private void drawNumber(Graphics2D g, Splat s, double scale, float alpha, double mx, double my)
	{
		BufferedImage[] digits = new BufferedImage[s.text.length()];
		int total = 0;
		for (int i = 0; i < digits.length; i++)
		{
			digits[i] = art.digit(s.text.charAt(i), theme, s.others, s.type, s.art);
			total += digits[i].getWidth() - ThemeArt.DIGIT_PAD * 2 + (i > 0 ? 1 : 0);
		}

		int x = (int) Math.round(mx - total / 2.0);
		g.setComposite(alpha(alpha));
		for (BufferedImage d : digits)
		{
			int inkW = d.getWidth() - ThemeArt.DIGIT_PAD * 2;
			int top = (int) Math.round(my - (d.getHeight() - ThemeArt.DIGIT_PAD * 2) / 2.0) - ThemeArt.DIGIT_PAD;
			if (scale == 1)
			{
				g.drawImage(d, x - ThemeArt.DIGIT_PAD, top, null);
			}
			else if (scale > 0)
			{
				double gx = x - ThemeArt.DIGIT_PAD + d.getWidth() / 2.0, gy = top + d.getHeight() / 2.0;
				AffineTransform before = g.getTransform();
				g.translate(mx + (gx - mx) * scale, my + (gy - my) * scale);
				g.scale(scale, scale);
				g.drawImage(d, -d.getWidth() / 2, -d.getHeight() / 2, null);
				g.setTransform(before);
			}
			x += inkW + 1;
		}
	}

	private static void particle(Graphics2D g, String name, double x, double y, Color color, double a)
	{
		int[][] shape = PARTICLES.get(name);
		if (shape == null || a <= 0)
		{
			return;
		}
		g.setComposite(alpha((float) a));
		g.setColor(color);
		int ix = (int) Math.round(x), iy = (int) Math.round(y);
		for (int[] d : shape)
		{
			g.fillRect(ix + d[0], iy + d[1], 1, 1);
		}
		if (name.equals("bubble"))
		{
			g.setColor(Color.WHITE);
			g.fillRect(ix - 1, iy - 1, 1, 1);
		}
	}

	@Value
	static class Pose
	{
		double sx;
		double sy;
		double rot;
		double dy;
		double alpha;
		boolean squish;
		double run;
		double popped;
		double drift;
	}

	private static Pose still(double sx, double sy, double rot)
	{
		return new Pose(sx, sy, rot, 0, 1, false, -1, -1, -1);
	}

	static Pose pose(Theme theme, boolean others, int age, int exit, boolean max)
	{
		if (others)
		{
			return exit < 0 ? still(1, 1, 0) : new Pose(1, 1, 0, 0, 1 - exit / (double) CALM_EXIT, false, -1, -1, -1);
		}
		if (exit < 0)
		{
			double[] k = keyframe(theme.getEnter(), age);
			double boost = max ? 1.4 : 1;
			return still(1 + (k[1] - 1) * boost, 1 + (k[2] - 1) * boost, k[3] * boost);
		}
		double t = exit / (double) theme.getExitCycles();
		switch (theme)
		{
			case SPRAY:
				return new Pose(1, 1, 0, 0, 1 - t * t, false, t, -1, -1);
			case GILDED:
				return new Pose(1, 1, 0, -8 * easeOut(t), 1 - t * t, false, -1, -1, -1);
			case BUBBLEGUM:
				return new Pose(1, 1, 0, 0, 0, false, -1, t, -1);
			case SLIME:
				return new Pose(1 + .5 * t, Math.max(.05, 1 - .85 * t), 0, 0, 1 - t * t * t, true, -1, -1, -1);
			case KITTY:
				return new Pose(1, 1, 0, -7 * Math.sin(Math.PI * t) + 10 * t * t, 1 - t * t, false, -1, -1, -1);
			default:
				return new Pose(1, 1, 0, 0, 1 - t, false, -1, -1, t);
		}
	}

	private static double[] keyframe(double[][] keys, double t)
	{
		if (t <= keys[0][0])
		{
			return keys[0];
		}
		for (int i = 1; i < keys.length; i++)
		{
			if (t < keys[i][0])
			{
				double[] a = keys[i - 1], b = keys[i];
				double f = (t - a[0]) / (b[0] - a[0]);
				double[] out = new double[a.length];
				for (int j = 0; j < a.length; j++)
				{
					out[j] = a[j] + (b[j] - a[j]) * f;
				}
				return out;
			}
		}
		return keys[keys.length - 1];
	}

	private static double easeOut(double t)
	{
		return 1 - (1 - t) * (1 - t);
	}

	private static AlphaComposite alpha(float a)
	{
		return a >= 1 ? AlphaComposite.SrcOver : AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, a));
	}
}
