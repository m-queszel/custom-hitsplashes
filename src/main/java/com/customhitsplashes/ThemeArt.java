package com.customhitsplashes;

import com.customhitsplashes.CustomHitsplashesConfig.Theme;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntFunction;
import javax.inject.Singleton;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;

@Singleton
class ThemeArt
{
	static final int DIGIT_PAD = 2;
	private static final int[][] N4 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
	private static final int[] WHITE = {255, 255, 255};
	private static final int[] BLACK = {0, 0, 0};
	private static final double[][] PUFFS = {{-.52, -.62, .4}, {.08, -.8, .44}, {.62, -.56, .36}};

	static final class Art
	{
		BufferedImage image;
		BufferedImage[] glint;
		double pivotY;
		double drawX;
		double drawY;
		double size;
		double bottom;
		int[] under;
		int[] vivid;
		final List<int[]> drips = new ArrayList<>();
		int[] dripColor;
	}

	private final Map<Integer, BufferedImage> sprites = new HashMap<>();
	private final Map<String, Art> arts = new HashMap<>();
	private final Map<Integer, int[]> colors = new HashMap<>();
	private final Map<String, BufferedImage> digits = new HashMap<>();
	private final Map<String, int[]> gameTexts = new HashMap<>();
	private Glyphs small;
	private Glyphs bold;

	boolean load(SpriteManager spriteManager, Collection<Integer> spriteIds)
	{
		for (int id : spriteIds)
		{
			if (!sprites.containsKey(id))
			{
				BufferedImage sprite = spriteManager.getSprite(id, 0);
				if (sprite == null)
				{
					return false;
				}
				sprites.put(id, sprite);
			}
		}
		return true;
	}

	boolean isLoaded(int spriteId)
	{
		return sprites.containsKey(spriteId);
	}

	BufferedImage sprite(int type)
	{
		return sprites.get(HitsplatDefs.sprite(type));
	}

	void clearGenerated()
	{
		arts.clear();
		colors.clear();
		digits.clear();
	}

	void clear()
	{
		clearGenerated();
		sprites.clear();
	}

	Art art(int type, Theme theme, boolean others, double hx, double hy)
	{
		boolean max = HitsplatDefs.isMax(type) && !others;
		String key = type + ":" + theme + ":" + others + ":" + Math.round(hx * 2) + ":" + Math.round(hy * 2);
		return arts.computeIfAbsent(key, k ->
			build(colors.computeIfAbsent(type, t -> typeColor(sprite(t))), theme, others, max, hx, hy, k.hashCode()));
	}

	static boolean inShape(Theme theme, double x, double y, double s, double k1, double k2)
	{
		switch (theme)
		{
			case SPRAY:
				return squircle(x, y, s, s, 2.4) || Math.hypot(x, y) <= s + spikes(Math.atan2(y, x), k1, k2);
			case GILDED:
				return Math.abs(x) <= s * 1.08 && Math.abs(y) <= s * .92 && Math.abs(x) + Math.abs(y) <= s * 1.75;
			case BUBBLEGUM:
				return squircle(x, y, s, s, 2.5);
			case SLIME:
				return y < 0 ? squircle(x, y, s * 1.05, s, 2.4) : squircle(x, y, s * 1.05, s * .85, 4);
			case KITTY:
				return squircle(x, y, s, s, 2.4);
			case STARDUST:
				if (squircle(x, y, s * 1.12, s * .85, 3))
				{
					return true;
				}
				for (double[] p : PUFFS)
				{
					if (Math.pow(x - p[0] * s, 2) + Math.pow(y - p[1] * s, 2) <= Math.pow(p[2] * s, 2))
					{
						return true;
					}
				}
				return false;
			default:
				return false;
		}
	}

	private static double spikes(double angle, double k1, double k2)
	{
		return 2 * Math.pow(Math.max(0, Math.sin(7 * angle + k1)), 4) + Math.pow(Math.max(0, Math.sin(11 * angle + k2)), 6);
	}

	private static boolean squircle(double x, double y, double a, double b, double n)
	{
		return Math.pow(Math.abs(x / a), n) + Math.pow(Math.abs(y / b), n) <= 1;
	}

	private static double squircleRadius(double angle, double n)
	{
		return Math.pow(Math.pow(Math.abs(Math.cos(angle)), n) + Math.pow(Math.abs(Math.sin(angle)), n), -1 / n);
	}

	private static double[][] ear(double s, int side)
	{
		return new double[][]{{side * .6 * s, -1.24 * s}, {side * .98 * s, -.5 * s}, {side * .22 * s, -.88 * s}};
	}

	private static double[][] shrink(double[][] t, double f)
	{
		double mx = (t[0][0] + t[1][0] + t[2][0]) / 3, my = (t[0][1] + t[1][1] + t[2][1]) / 3;
		double[][] out = new double[3][];
		for (int k = 0; k < 3; k++)
		{
			out[k] = new double[]{mx + (t[k][0] - mx) * f, my + (t[k][1] - my) * f};
		}
		return out;
	}

	private static boolean inTriangle(double x, double y, double[][] t)
	{
		double d1 = cross(x, y, t[0], t[1]), d2 = cross(x, y, t[1], t[2]), d3 = cross(x, y, t[2], t[0]);
		return !((d1 < 0 || d2 < 0 || d3 < 0) && (d1 > 0 || d2 > 0 || d3 > 0));
	}

	private static double cross(double x, double y, double[] a, double[] b)
	{
		return (x - b[0]) * (a[1] - b[1]) - (a[0] - b[0]) * (y - b[1]);
	}

	static Art build(int[] c, Theme theme, boolean others, boolean max, double hx, double hy, long seed)
	{
		Random rand = new Random(seed);
		double k1 = rand.nextDouble() * 6, k2 = rand.nextDouble() * 6;
		if (theme == Theme.GILDED)
		{
			hx += 2;
			hy += 1;
		}
		double size = theme.getMinSize();
		while (size < 40 && !coversBox(theme, size, hx, hy, k1, k2))
		{
			size += .25;
		}
		double s = size;

		int R = (int) Math.ceil(s * 1.6) + 8, W = 2 * R, n = W * W;
		double[] rx = new double[n], ry = new double[n];
		boolean[] M = new boolean[n];
		double[][][] ears = theme == Theme.KITTY ? new double[][][]{ear(s, -1), ear(s, 1)} : new double[0][][];
		for (int i = 0; i < n; i++)
		{
			rx[i] = i % W + .5 - R;
			ry[i] = i / W + .5 - R;
			M[i] = inShape(theme, rx[i], ry[i], s, k1, k2);
			for (double[][] e : ears)
			{
				M[i] |= inTriangle(rx[i], ry[i], e);
			}
		}

		int[] D = new int[n];
		List<Integer> front = new ArrayList<>();
		for (int i = 0; i < n; i++)
		{
			if (M[i] && touches(M, W, i, false))
			{
				D[i] = 1;
				front.add(i);
			}
		}
		for (int k = 2; k <= 4; k++)
		{
			List<Integer> next = new ArrayList<>();
			for (int i : front)
			{
				for (int[] d : N4)
				{
					int j = i + d[1] * W + d[0];
					if (j >= 0 && j < n && M[j] && D[j] == 0)
					{
						D[j] = k;
						next.add(j);
					}
				}
			}
			front = next;
		}
		for (int i = 0; i < n; i++)
		{
			if (M[i] && D[i] == 0)
			{
				D[i] = 5;
			}
		}

		int[] px = new int[n];
		Paint paint = new Paint(px, M, W, R);
		Art art = new Art();
		boolean deco = !others;

		switch (theme)
		{
			case SPRAY:
			{
				int[] light = tone(c, .85f, .57f), mid = tone(c, .8f, .42f), dark = tone(c, .75f, .26f), ink = {20, 18, 22};
				paint.fill(i -> D[i] == 1 ? ink : D[i] == 2 && rx[i] + ry[i] > 0 ? dark : mid);
				if (deco)
				{
					for (int k = 0; k < 3; k++)
					{
						paint.set(-.5 * s + k, -.42 * s + k * .6, light);
					}
					for (int i = 0; i < n; i++)
					{
						if (M[i])
						{
							continue;
						}
						double a = Math.atan2(ry[i], rx[i]);
						double out = Math.hypot(rx[i], ry[i]) - Math.max(s * squircleRadius(a, 2.4), s + spikes(a, k1, k2));
						double chance = out <= 1 ? .35 : out <= 2 ? .15 : out <= 3 ? .05 : 0;
						if (rand.nextDouble() < chance)
						{
							px[i] = argb(rand.nextBoolean() ? light : mid);
						}
					}
					int last = -99;
					for (int k = 0; k < 3; k++)
					{
						int x = (int) Math.round((rand.nextDouble() * 1.2 - .6) * s);
						if (Math.abs(x - last) < 3)
						{
							x += 4;
						}
						last = x;
						int tip = paint.drip(x + R, 4 + rand.nextInt(5), mid, dark, k == 0);
						if (tip >= 0)
						{
							art.drips.add(new int[]{x + R, tip});
						}
					}
				}
				art.dripColor = mid;
				break;
			}
			case GILDED:
			{
				int[] enamel = tone(c, .55f, .34f), enamelDark = tone(c, .55f, .25f);
				int[] goldLight = rgb(0xf2d77c), gold = rgb(0xd4ad4b), goldDark = rgb(0x8f6a22), frame = rgb(0x3a2606);
				paint.fill(i -> D[i] == 1 ? frame : D[i] == 2 ? (rx[i] + ry[i] < 0 ? goldLight : goldDark) : D[i] == 3 ? gold
					: D[i] == 4 && ry[i] < 0 ? enamelDark : enamel);
				if (deco)
				{
					for (int[] q : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}})
					{
						paint.set(q[0] * (s * 1.08 - 4.5), q[1] * (s * .92 - 4.5), goldLight);
					}
				}
				break;
			}
			case BUBBLEGUM:
			{
				int[] pastel = tone(c, .7f, .8f), shade = tone(c, .62f, .68f), edge = tone(c, .6f, .36f);
				paint.fill(i -> D[i] == 1 ? edge : (rx[i] + ry[i]) * .7 > s * .45 && D[i] <= 3 ? shade : pastel);
				if (deco)
				{
					double bx = .82 * s, by = -.82 * s, br = Math.max(2.2, s * .24);
					for (int i = 0; i < n; i++)
					{
						double r = Math.hypot(rx[i], ry[i]), a = Math.atan2(ry[i], rx[i]), rb = Math.hypot(rx[i] - bx, ry[i] - by);
						if (M[i] && r > s - 3.6 && r < s - 2.1 && a > -2.75 && a < -1.85)
						{
							px[i] = argb(WHITE);
						}
						else if (!M[i] && rb <= br)
						{
							px[i] = argb(rb > br - 1 ? edge : pastel);
						}
					}
					paint.set(-.68 * s, -.08 * s, WHITE);
					paint.set(bx - br * .4, by - br * .4, WHITE);
				}
				break;
			}
			case SLIME:
			{
				int[] top = tone(c, .7f, .66f), bottom = tone(c, .72f, .36f), edge = tone(c, .6f, .22f);
				paint.fill(i -> D[i] == 1 ? edge : mix(top, bottom, clamp((ry[i] + s) / (1.8 * s))));
				if (deco)
				{
					for (int i = 0; i < n; i++)
					{
						if (M[i] && D[i] == 2 && rx[i] < -.15 * s && ry[i] < -.3 * s)
						{
							px[i] = argb(WHITE);
						}
					}
					paint.set(-.28 * s, -.62 * s, WHITE);
					paint.drip((int) Math.floor(-.45 * s + R), 2, bottom, edge, false);
					paint.drip((int) Math.floor(.32 * s + R), 3, bottom, edge, false);
				}
				break;
			}
			case KITTY:
			{
				int[] fur = tone(c, .6f, .8f), furShade = tone(c, .55f, .7f), edge = tone(c, .55f, .3f), pink = rgb(0xff9cc2), innerEar = mix(pink, fur, .25);
				double[][][] innerEars = {shrink(ears[0], .45), shrink(ears[1], .45)};
				paint.fill(i ->
				{
					if (D[i] == 1)
					{
						return edge;
					}
					boolean inEar = !squircle(rx[i], ry[i], s, s, 2.4) && (inTriangle(rx[i], ry[i], innerEars[0]) || inTriangle(rx[i], ry[i], innerEars[1]));
					return inEar ? innerEar : ry[i] > s * .55 && D[i] <= 2 ? furShade : fur;
				});
				if (deco)
				{
					for (int side : new int[]{-1, 1})
					{
						for (double[] w : new double[][]{{.12, -.25}, {.36, .2}})
						{
							for (int k = 1; k <= 2; k++)
							{
								paint.set(side * (s + k - .5), w[0] * s + w[1] * k, edge);
							}
						}
						paint.set(side * .55 * s - .5, .52 * s, pink);
						paint.set(side * .55 * s + .5, .52 * s, pink);
					}
				}
				break;
			}
			case STARDUST:
			{
				int[] night = mix(rgb(0x1c1838), c, .2), deep = mix(rgb(0x0d0a1f), c, .12), glow = tone(c, .85f, .66f);
				paint.fill(i -> D[i] == 1 ? glow : D[i] == 2 ? mix(glow, night, .55) : mix(night, deep, clamp((ry[i] + s * .3) / (s * 1.3))));
				for (int i = 0; i < n; i++)
				{
					if (!M[i] && touches(M, W, i, true))
					{
						px[i] = argb(glow, 90);
					}
				}
				if (deco)
				{
					for (double[] st : new double[][]{{-.66, -.02, 1}, {.58, -.34, 0}, {-.28, .6, 1}, {.66, .42, 0}, {-.08, -.72, 1}})
					{
						int i = paint.index(st[0] * s, st[1] * s);
						if (i >= 0 && M[i] && D[i] >= 3)
						{
							px[i] = argb(st[2] == 1 ? WHITE : rgb(0xfff3a3));
						}
					}
				}
				break;
			}
			default:
				break;
		}

		if (max)
		{
			int[] accent = rgb(theme.getAccent());
			List<Integer> ring = new ArrayList<>();
			for (int i = 0; i < n; i++)
			{
				if (!M[i] && touches(M, W, i, true))
				{
					ring.add(i);
					px[i] = argb(accent);
				}
			}
			for (int i : ring)
			{
				for (int[] d : N4)
				{
					int j = i + d[1] * W + d[0];
					if (j >= 0 && j < n && !M[j] && (px[j] >>> 24) == 0)
					{
						px[j] = argb(accent, 110);
					}
				}
			}
		}

		if (others)
		{
			for (int i = 0; i < n; i++)
			{
				if ((px[i] >>> 24) != 0)
				{
					int[] col = rgb(px[i]);
					int l = luma(col);
					px[i] = argb(mix(mix(col, new int[]{l, l, l}, .85), BLACK, .12), px[i] >>> 24);
				}
			}
		}

		art.image = image(px, W, W);
		if (theme == Theme.GILDED && deco)
		{
			art.glint = new BufferedImage[7];
			for (int f = 0; f < art.glint.length; f++)
			{
				double band = -2 * s + f * (4 * s / 6);
				int[] frame = px.clone();
				for (int i = 0; i < n; i++)
				{
					if (M[i] && D[i] >= 2 && Math.abs((rx[i] - ry[i]) - band) < 1.3)
					{
						frame[i] = argb(mix(rgb(frame[i]), WHITE, .6));
					}
				}
				art.glint[f] = image(frame, W, W);
			}
		}

		double numberOffset = theme.getNumberOffset() * s;
		long ur = 0, ug = 0, ub = 0, un = 0;
		for (int i = 0; i < n; i++)
		{
			if (M[i] && Math.hypot(rx[i], ry[i] - numberOffset) <= s * .5)
			{
				int[] col = rgb(px[i]);
				ur += col[0];
				ug += col[1];
				ub += col[2];
				un++;
			}
		}

		art.pivotY = -numberOffset;
		art.drawX = -R;
		art.drawY = -numberOffset - R;
		art.size = s;
		art.bottom = s * theme.getBottom();
		art.under = un == 0 ? c : new int[]{(int) (ur / un), (int) (ug / un), (int) (ub / un)};
		art.vivid = c;
		if (art.dripColor == null)
		{
			art.dripColor = c;
		}
		return art;
	}

	static boolean coversBox(Theme theme, double s, double hx, double hy, double k1, double k2)
	{
		double oy = theme.getNumberOffset() * s;
		for (double t = -1; t <= 1.0001; t += .05)
		{
			if (!inShape(theme, t * hx, oy - hy, s, k1, k2) || !inShape(theme, t * hx, oy + hy, s, k1, k2)
				|| !inShape(theme, -hx, oy + t * hy, s, k1, k2) || !inShape(theme, hx, oy + t * hy, s, k1, k2))
			{
				return false;
			}
		}
		return true;
	}

	static int[] typeColor(BufferedImage sprite)
	{
		long r = 0, g = 0, b = 0, count = 0;
		for (int y = 0; y < sprite.getHeight(); y++)
		{
			for (int x = 0; x < sprite.getWidth(); x++)
			{
				int argb = sprite.getRGB(x, y);
				int[] c = rgb(argb);
				if ((argb >>> 24) > 40 && luma(c) >= 55)
				{
					r += c[0];
					g += c[1];
					b += c[2];
					count++;
				}
			}
		}
		int[] avg = count == 0 ? new int[]{180, 40, 30} : new int[]{(int) (r / count), (int) (g / count), (int) (b / count)};
		float[] hsl = toHsl(avg);
		return fromHsl(new float[]{hsl[0], Math.max(hsl[1], .6f), Math.min(Math.max(hsl[2], .45f), .58f)});
	}

	private static boolean touches(boolean[] m, int w, int i, boolean wanted)
	{
		for (int[] d : N4)
		{
			int j = i + d[1] * w + d[0];
			if ((j >= 0 && j < m.length && m[j]) == wanted)
			{
				return true;
			}
		}
		return false;
	}

	private static final class Paint
	{
		private final int[] px;
		private final boolean[] m;
		private final int w;
		private final int r;

		Paint(int[] px, boolean[] m, int w, int r)
		{
			this.px = px;
			this.m = m;
			this.w = w;
			this.r = r;
		}

		void fill(IntFunction<int[]> color)
		{
			for (int i = 0; i < px.length; i++)
			{
				if (m[i])
				{
					px[i] = argb(color.apply(i));
				}
			}
		}

		int index(double x, double y)
		{
			int ix = (int) Math.floor(x + r), iy = (int) Math.floor(y + r);
			return ix < 0 || iy < 0 || ix >= w || iy >= px.length / w ? -1 : iy * w + ix;
		}

		void set(double x, double y, int[] col)
		{
			int i = index(x, y);
			if (i >= 0)
			{
				px[i] = argb(col);
			}
		}

		void put(int ix, int iy, int[] col)
		{
			if (ix >= 0 && iy >= 0 && ix < w && iy < px.length / w)
			{
				px[iy * w + ix] = argb(col);
			}
		}

		int drip(int col, int length, int[] body, int[] tip, boolean wide)
		{
			int bottom = -1;
			for (int y = px.length / w - 1; y >= 0 && bottom < 0 && col >= 0 && col < w; y--)
			{
				if (m[y * w + col])
				{
					bottom = y;
				}
			}
			if (bottom < 0)
			{
				return -1;
			}
			for (int j = 1; j < length; j++)
			{
				put(col, bottom + j, body);
				if (wide)
				{
					put(col + 1, bottom + j, body);
				}
			}
			put(col, bottom + length, tip);
			return bottom + length;
		}
	}

	private void ensureGlyphs()
	{
		if (small == null)
		{
			small = new Glyphs(FontManager.getRunescapeSmallFont());
			bold = new Glyphs(FontManager.getRunescapeBoldFont());
		}
	}

	int[] numberSize(String text, boolean others)
	{
		ensureGlyphs();
		Glyphs glyphs = others ? small : bold;
		int w = 0;
		for (int i = 0; i < text.length(); i++)
		{
			w += glyphs.width(text.charAt(i)) + (others ? 0 : 1) + (i > 0 ? 1 : 0);
		}
		return new int[]{w + 2, glyphs.height + 2};
	}

	int[] gameText(String text)
	{
		return gameTexts.computeIfAbsent(text, t ->
		{
			BufferedImage img = new BufferedImage(96, 48, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			g.setFont(FontManager.getRunescapeSmallFont());
			FontMetrics fm = g.getFontMetrics();
			g.setColor(Color.WHITE);
			g.drawString(t, 10, 30);
			g.drawString(t, 11, 31);
			g.dispose();
			int x0 = 96, y0 = 48, x1 = -1, y1 = -1;
			for (int y = 0; y < 48; y++)
			{
				for (int x = 0; x < 96; x++)
				{
					if ((img.getRGB(x, y) >>> 24) > 110)
					{
						x0 = Math.min(x0, x);
						x1 = Math.max(x1, x);
						y0 = Math.min(y0, y);
						y1 = Math.max(y1, y);
					}
				}
			}
			return x1 < 0 ? new int[]{fm.stringWidth(t), 0, -8, 0, 0} : new int[]{fm.stringWidth(t), x0 - 10, y0 - 30, x1 - 10, y1 - 30};
		});
	}

	BufferedImage digit(char ch, Theme theme, boolean others, int type, Art art)
	{
		String key = ch + ":" + theme + ":" + others + ":" + type;
		BufferedImage cached = digits.get(key);
		if (cached != null)
		{
			return cached;
		}
		ensureGlyphs();
		BufferedImage image = digitImage(others ? small : bold, ch, theme, others, HitsplatDefs.isDamage(type), art);
		digits.put(key, image);
		return image;
	}

	static BufferedImage digitImage(Glyphs glyphs, char ch, Theme theme, boolean others, boolean damage, Art art)
	{
		boolean[] g0 = glyphs.mask(ch);
		int w0 = glyphs.width(ch), H = glyphs.height, W = w0 + (others ? 0 : 1);
		boolean[] m = new boolean[W * H];
		for (int y = 0; y < H; y++)
		{
			for (int x = 0; x < W; x++)
			{
				m[y * W + x] = x < w0 && g0[y * w0 + x] || !others && x > 0 && g0[y * w0 + x - 1];
			}
		}
		Glyph g = new Glyph(m, W, H);
		int cw = W + DIGIT_PAD * 2, chh = H + DIGIT_PAD * 2;
		int[] px = new int[cw * chh];
		int[] c = art.vivid;
		int[] greyFill = readable(new int[]{200, 200, 200}, art.under);

		for (int y = -DIGIT_PAD; y < H + DIGIT_PAD; y++)
		{
			for (int x = -DIGIT_PAD; x < W + DIGIT_PAD; x++)
			{
				boolean in = g.in(x, y), edge = g.outline(x, y);
				int col = 0;
				if (others)
				{
					col = edge ? argb(luma(greyFill) > 128 ? new int[]{18, 18, 20} : new int[]{235, 235, 235}) : in ? argb(greyFill) : 0;
				}
				else
				{
					switch (theme)
					{
						case SPRAY:
							col = edge ? argb(new int[]{14, 12, 16}) : in ? argb(WHITE) : g.outline(x - 1, y - 1) ? argb(damage ? rgb(theme.getAccent()) : tone(c, .9f, .6f)) : 0;
							break;
						case GILDED:
							col = edge ? argb(rgb(0x3a2606)) : in ? argb(rgb(y * 2 < H ? 0xfff4d8 : 0xf2d68a)) : g.body(x, y - 1) ? argb(BLACK, 120) : 0;
							break;
						case BUBBLEGUM:
						{
							int[] fill = tone(c, .75f, .3f);
							col = edge ? argb(WHITE) : in ? argb(y * 2 < H ? mix(fill, WHITE, .18) : fill) : g.body(x, y - 1) ? argb(BLACK, 80) : 0;
							break;
						}
						case SLIME:
						{
							int[] outline = tone(c, .6f, .2f);
							col = edge ? argb(outline) : in ? argb(y * 2 < H ? WHITE : mix(outline, WHITE, .82)) : 0;
							break;
						}
						case KITTY:
							col = in ? argb(readable(tone(c, .55f, .22f), art.under)) : g.in(x, y - 1) ? argb(WHITE, 170) : 0;
							break;
						case STARDUST:
							col = edge ? argb(rgb(0x0d0a1f)) : in ? argb(WHITE) : g.glow(x, y) ? argb(tone(c, .85f, .66f), 170) : 0;
							break;
						default:
							col = in ? argb(WHITE) : 0;
					}
				}
				px[(y + DIGIT_PAD) * cw + x + DIGIT_PAD] = col;
			}
		}
		return image(px, cw, chh);
	}

	private interface PixelTest
	{
		boolean at(int x, int y);
	}

	private static final class Glyph
	{
		private final boolean[] m;
		private final int w;
		private final int h;

		Glyph(boolean[] m, int w, int h)
		{
			this.m = m;
			this.w = w;
			this.h = h;
		}

		boolean in(int x, int y)
		{
			return x >= 0 && y >= 0 && x < w && y < h && m[y * w + x];
		}

		boolean outline(int x, int y)
		{
			return !in(x, y) && near(x, y, this::in);
		}

		boolean body(int x, int y)
		{
			return in(x, y) || outline(x, y);
		}

		boolean glow(int x, int y)
		{
			return !body(x, y) && near(x, y, this::outline);
		}

		private static boolean near(int x, int y, PixelTest test)
		{
			for (int dy = -1; dy <= 1; dy++)
			{
				for (int dx = -1; dx <= 1; dx++)
				{
					if ((dx != 0 || dy != 0) && test.at(x + dx, y + dy))
					{
						return true;
					}
				}
			}
			return false;
		}
	}

	static final class Glyphs
	{
		private final boolean[][] masks = new boolean[10][];
		private final int[] widths = new int[10];
		final int height;

		Glyphs(Font font)
		{
			int size = 32;
			int top = size, bottom = -1;
			int[][] raw = new int[10][];
			int[] left = new int[10], right = new int[10];
			for (int d = 0; d < 10; d++)
			{
				BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = img.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
				g.setFont(font);
				FontMetrics fm = g.getFontMetrics();
				g.setColor(Color.WHITE);
				g.drawString(String.valueOf((char) ('0' + d)), 4, 4 + fm.getAscent());
				g.dispose();
				raw[d] = img.getRGB(0, 0, size, size, null, 0, size);
				left[d] = size;
				right[d] = -1;
				for (int i = 0; i < raw[d].length; i++)
				{
					if ((raw[d][i] >>> 24) > 110)
					{
						int x = i % size, y = i / size;
						left[d] = Math.min(left[d], x);
						right[d] = Math.max(right[d], x);
						top = Math.min(top, y);
						bottom = Math.max(bottom, y);
					}
				}
			}
			height = Math.max(1, bottom - top + 1);
			for (int d = 0; d < 10; d++)
			{
				int w = Math.max(1, right[d] - left[d] + 1);
				widths[d] = w;
				masks[d] = new boolean[w * height];
				for (int y = 0; y < height; y++)
				{
					for (int x = 0; x < w; x++)
					{
						int sx = x + left[d], sy = y + top;
						masks[d][y * w + x] = sx < size && sy < size && (raw[d][sy * size + sx] >>> 24) > 110;
					}
				}
			}
		}

		boolean[] mask(char c)
		{
			return masks[c - '0'];
		}

		int width(char c)
		{
			return widths[c - '0'];
		}
	}

	static int[] readable(int[] color, int[] background)
	{
		float[] hsl = toHsl(color);
		boolean lighten = luminance(background) < .2;
		int[] out = color;
		for (int k = 0; k < 16 && contrast(out, background) < 4.5; k++)
		{
			hsl[2] = lighten ? Math.min(1, hsl[2] + .05f) : Math.max(0, hsl[2] - .05f);
			out = fromHsl(hsl);
		}
		return out;
	}

	static double contrast(int[] a, int[] b)
	{
		double x = luminance(a), y = luminance(b);
		return (Math.max(x, y) + .05) / (Math.min(x, y) + .05);
	}

	private static double luminance(int[] c)
	{
		return .2126 * channel(c[0]) + .7152 * channel(c[1]) + .0722 * channel(c[2]);
	}

	private static double channel(int v)
	{
		double s = v / 255.0;
		return s <= .03928 ? s / 12.92 : Math.pow((s + .055) / 1.055, 2.4);
	}

	static int[] tone(int[] c, float s, float l)
	{
		return fromHsl(new float[]{toHsl(c)[0], s, l});
	}

	private static float[] toHsl(int[] c)
	{
		float r = c[0] / 255f, g = c[1] / 255f, b = c[2] / 255f;
		float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), l = (max + min) / 2;
		if (max == min)
		{
			return new float[]{0, 0, l};
		}
		float d = max - min, s = l > .5f ? d / (2 - max - min) : d / (max + min);
		float hue = max == r ? (g - b) / d + (g < b ? 6 : 0) : max == g ? (b - r) / d + 2 : (r - g) / d + 4;
		return new float[]{hue / 6, s, l};
	}

	private static int[] fromHsl(float[] hsl)
	{
		float h = hsl[0], s = hsl[1], l = hsl[2];
		if (s == 0)
		{
			int v = Math.round(l * 255);
			return new int[]{v, v, v};
		}
		float q = l < .5f ? l * (1 + s) : l + s - l * s, p = 2 * l - q;
		return new int[]{Math.round(hue(p, q, h + 1 / 3f) * 255), Math.round(hue(p, q, h) * 255), Math.round(hue(p, q, h - 1 / 3f) * 255)};
	}

	private static float hue(float p, float q, float t)
	{
		t = (t + 1) % 1;
		return t < 1 / 6f ? p + (q - p) * 6 * t : t < .5f ? q : t < 2 / 3f ? p + (q - p) * (2 / 3f - t) * 6 : p;
	}

	static int[] mix(int[] a, int[] b, double t)
	{
		return new int[]{(int) Math.round(a[0] + (b[0] - a[0]) * t), (int) Math.round(a[1] + (b[1] - a[1]) * t), (int) Math.round(a[2] + (b[2] - a[2]) * t)};
	}

	private static double clamp(double t)
	{
		return Math.max(0, Math.min(1, t));
	}

	private static int luma(int[] c)
	{
		return (299 * c[0] + 587 * c[1] + 114 * c[2]) / 1000;
	}

	static int[] rgb(int argb)
	{
		return new int[]{(argb >> 16) & 0xff, (argb >> 8) & 0xff, argb & 0xff};
	}

	private static int argb(int[] c)
	{
		return argb(c, 255);
	}

	private static int argb(int[] c, int alpha)
	{
		return alpha << 24 | c[0] << 16 | c[1] << 8 | c[2];
	}

	private static BufferedImage image(int[] px, int w, int h)
	{
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		out.setRGB(0, 0, w, h, px, 0, w);
		return out;
	}
}
