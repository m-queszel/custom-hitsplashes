package com.customhitsplashes;

import com.google.inject.Provides;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.SpritePixels;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Custom Hitsplashes",
	description = "Animated, restyled hitsplats built from the game's own art",
	tags = {"hitsplat", "hitsplash", "damage", "combat", "animation", "cosmetic"}
)
public class CustomHitsplashesPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private CustomHitsplashesConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private HitsplatOverlay overlay;

	@Inject
	private ThemeArt art;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	private boolean ready;
	private final Map<Integer, SpritePixels> blanks = new HashMap<>();
	private final Set<Integer> blanked = new HashSet<>();

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		clientThread.invoke(this::apply);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		clientThread.invoke(() ->
		{
			ready = false;
			if (!blanked.isEmpty())
			{
				setBlanked(Set.of());
				tell("log out and back in to bring back the game's own hitsplats.");
			}
			overlay.setTheme(null);
			overlay.clear();
			art.clear();
		});
	}

	private boolean apply()
	{
		art.clearGenerated();
		overlay.restyle(config.theme());
		ready = art.load(spriteManager, HitsplatDefs.spriteIds(true)) && makeBlanks();
		if (!ready)
		{
			return false;
		}
		Set<Integer> wanted = HitsplatDefs.spriteIds(config.skinOthers());
		if (!wanted.equals(blanked))
		{
			setBlanked(wanted);
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				tell("log out and back in for the change to show.");
			}
		}
		return true;
	}

	private boolean makeBlanks()
	{
		for (int id : HitsplatDefs.spriteIds(true))
		{
			if (blanks.containsKey(id))
			{
				continue;
			}
			SpritePixels[] frames = client.getSprites(client.getIndexSprites(), id, 0);
			if (frames == null || frames.length == 0 || frames[0] == null)
			{
				return false;
			}
			SpritePixels sprite = frames[0];
			SpritePixels blank = client.createSpritePixels(new int[sprite.getWidth() * sprite.getHeight()], sprite.getWidth(), sprite.getHeight());
			blank.setMaxWidth(sprite.getMaxWidth());
			blank.setMaxHeight(sprite.getMaxHeight());
			blank.setOffsetX(sprite.getOffsetX());
			blank.setOffsetY(sprite.getOffsetY());
			blanks.put(id, blank);
		}
		return true;
	}

	private void setBlanked(Set<Integer> ids)
	{
		Map<Integer, SpritePixels> overrides = client.getSpriteOverrides();
		blanked.forEach(id -> overrides.remove(id, blanks.get(id)));
		blanked.clear();
		ids.forEach(id -> overrides.put(id, blanks.get(id)));
		blanked.addAll(ids);
	}

	private void tell(String message)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage("Custom Hitsplashes: " + message)
			.build());
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CustomHitsplashesConfig.GROUP.equals(event.getGroup()))
		{
			clientThread.invoke(this::apply);
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		Hitsplat hitsplat = event.getHitsplat();
		int type = HitsplatDefs.resolve(hitsplat.getHitsplatType(), client::getVarbitValue);
		if (type < 0 || !ready || !blanked.contains(HitsplatDefs.sprite(type)))
		{
			return;
		}
		boolean others = HitsplatDefs.isOthers(hitsplat.getHitsplatType());
		overlay.add(event.getActor(), type, hitsplat.getAmount(), hitsplat.getDisappearsOnGameCycle(), others, others && !config.skinOthers());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOADING || state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			overlay.clear();
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		overlay.remove(event.getActor());
	}

	@Subscribe
	public void onPlayerDespawned(PlayerDespawned event)
	{
		overlay.remove(event.getActor());
	}

	@Provides
	CustomHitsplashesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CustomHitsplashesConfig.class);
	}
}
