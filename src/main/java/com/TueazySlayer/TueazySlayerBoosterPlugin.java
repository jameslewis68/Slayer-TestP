package com.TueazySlayer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@PluginDescriptor(
        name = "TueazySlayerBooster"
)
public class TueazySlayerBoosterPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private TueazySlayerBoosterConfig config;

    @Inject
    private OverlayManager overlayManager;

    private Map<String, List<WorldPoint>> monsterLocations;
    private WorldPoint currentTarget;

    private SlayerOverlay slayerOverlay;

    @Override
    protected void startUp() throws Exception
    {
        monsterLocations = loadMonsterLocations();
        slayerOverlay = new SlayerOverlay();
        overlayManager.add(slayerOverlay);
        log.info("TueazySlayerBooster started!");
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(slayerOverlay);
        log.info("TueazySlayerBooster stopped!");
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        String msg = event.getMessage().toLowerCase();

        // Detect new task assignment or checking current task with gem
        if (msg.contains("assigned") || msg.contains("your slayer task") || msg.contains("your task is"))
        {
            for (String monster : monsterLocations.keySet())
            {
                if (msg.contains(monster.toLowerCase()))
                {
                    log.info("Detected Slayer task: " + monster);
                    currentTarget = monsterLocations.get(monster).get(0);
                    break;
                }
            }
        }
    }

    private Map<String, List<WorldPoint>> loadMonsterLocations()
    {
        try (InputStream in = TueazySlayerBoosterPlugin.class.getResourceAsStream("/slayer_tasks.json"))
        {
            if (in == null)
            {
                log.error("slayer_tasks.json not found!");
                return Collections.emptyMap();
            }

            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, List<Map<String, Integer>>> raw = new Gson().fromJson(json,
                    new TypeToken<Map<String, List<Map<String, Integer>>>>(){}.getType());

            Map<String, List<WorldPoint>> out = new HashMap<>();
            for (Map.Entry<String, List<Map<String, Integer>>> entry : raw.entrySet())
            {
                List<WorldPoint> points = new ArrayList<>();
                for (Map<String, Integer> coords : entry.getValue())
                {
                    points.add(new WorldPoint(coords.get("x"), coords.get("y"), coords.get("plane")));
                }
                out.put(entry.getKey().toLowerCase(), points);
            }
            return out;
        }
        catch (Exception e)
        {
            log.error("Error loading slayer_tasks.json", e);
            return Collections.emptyMap();
        }
    }

    @Provides
    TueazySlayerBoosterConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(TueazySlayerBoosterConfig.class);
    }

    private class SlayerOverlay extends Overlay
    {
        private final PanelComponent panel = new PanelComponent();

        SlayerOverlay()
        {
            setPosition(OverlayPosition.TOP_LEFT);
            setLayer(OverlayLayer.ABOVE_WIDGETS);
            panel.setPreferredSize(new Dimension(150, 70));
        }

        @Override
        public Dimension render(Graphics2D graphics)
        {
            panel.getChildren().clear();

            if (currentTarget != null)
            {
                panel.getChildren().add(TitleComponent.builder()
                        .text("Slayer Target Coordinates")
                        .build());

                panel.getChildren().add(LineComponent.builder()
                        .left("X")
                        .right(String.valueOf(currentTarget.getX()))
                        .build());

                panel.getChildren().add(LineComponent.builder()
                        .left("Y")
                        .right(String.valueOf(currentTarget.getY()))
                        .build());

                panel.getChildren().add(LineComponent.builder()
                        .left("Plane")
                        .right(String.valueOf(currentTarget.getPlane()))
                        .build());
            }

            return panel.render(graphics);
        }
    }
}
