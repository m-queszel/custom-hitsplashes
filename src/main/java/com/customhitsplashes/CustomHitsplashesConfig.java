package com.customhitsplashes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(CustomHitsplashesConfig.GROUP)
public interface CustomHitsplashesConfig extends Config
{
	String GROUP = "customhitsplashes";

	@RequiredArgsConstructor
	@Getter
	enum Theme
	{
		CLASSIC("Classic", 0, null, 0, 0, 0, 0, new double[][]{{0, 1, 1, 0}}),
		SPRAY("Spray Paint", 0xff3d8b, "dot", 9, 0, 1, 22, new double[][]{{0, 1.35, 1.35, 0}, {2, 1, 1, 0}}),
		GILDED("Gilded", 0xffe08a, "star", 9.5, 0, .92, 18, new double[][]{{0, 1, 1, 0}}),
		BUBBLEGUM("Bubblegum", 0xff8fc8, "bubble", 10, 0, 1, 12, new double[][]{{0, 1, 1, 0}, {1.5, 1.14, 1.14, 0}, {4, 1, 1, 0}}),
		SLIME("Slime", 0xc8fff2, "bubble", 9.5, .12, .85, 14, new double[][]{{0, 1.3, 1, 0}, {3, 1, 1, 0}}),
		KITTY("Kitty", 0xff9cc2, "heart", 9.5, .12, 1, 16, new double[][]{{0, 1.18, 1.18, -.14}, {3, 1, 1, 0}}),
		STARDUST("Stardust", 0xfff3a3, "star", 9, .08, .85, 18, new double[][]{{0, 1, 1, 0}});

		private final String name;
		private final int accent;
		private final String fx;
		private final double minSize;
		private final double numberOffset;
		private final double bottom;
		private final int exitCycles;
		private final double[][] enter;

		@Override
		public String toString()
		{
			return name;
		}
	}

	@ConfigItem(
		keyName = "theme",
		name = "Theme",
		description = "How your hitsplats look. Classic looks exactly like the game's own.",
		position = 0
	)
	default Theme theme()
	{
		return Theme.SPRAY;
	}

	@ConfigItem(
		keyName = "skinOthers",
		name = "Skin other players' hits",
		description = "Other players' hits use a grey version of the theme. Turn off to keep them looking like the game's own (takes effect after logging out and back in).",
		position = 1
	)
	default boolean skinOthers()
	{
		return true;
	}
}
