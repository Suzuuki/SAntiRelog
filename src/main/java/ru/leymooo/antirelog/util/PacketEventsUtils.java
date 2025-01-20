package ru.leymooo.antirelog.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetCooldown;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.leymooo.antirelog.config.Settings;
import ru.leymooo.antirelog.manager.CooldownManager;
import ru.leymooo.antirelog.manager.CooldownManager.CooldownType;
import ru.leymooo.antirelog.manager.PvPManager;

import java.util.Arrays;
import java.util.List;

public class PacketEventsUtils {

    public static void sendCooldownPacket(Player player, Material material, int ticks) {
        WrapperPlayServerSetCooldown packet = new WrapperPlayServerSetCooldown(SpigotConversionUtil.fromBukkitItemMaterial(material), ticks);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    public static void createListener(CooldownManager cooldownManager, PvPManager pvPManager) {
        Settings settings = cooldownManager.getSettings();
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract(PacketListenerPriority.HIGH) {
            final List<CooldownType> types = Arrays.asList(CooldownType.CHORUS, CooldownType.ENDER_PEARL);
            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (!event.getPacketType().equals(PacketType.Play.Server.SET_COOLDOWN)) return;

                final WrapperPlayServerSetCooldown wrapper = new WrapperPlayServerSetCooldown(event);

                Material material = SpigotConversionUtil.toBukkitItemMaterial(wrapper.getItem());

                int duration = wrapper.getCooldownTicks();
                duration = duration * 50;
                for (CooldownType cooldownType : types) {
                    if (material == cooldownType.getMaterial()) {
                        boolean hasCooldown = cooldownManager.hasCooldown(event.getPlayer(), cooldownType, cooldownType.getCooldown(settings) * 1000);
                        if (hasCooldown) {
                            long remaning = cooldownManager.getRemaining(event.getPlayer(), cooldownType, cooldownType.getCooldown(settings) * 1000);
                            if (Math.abs(remaning - duration) > 100) {
                                if (!pvPManager.isPvPModeEnabled() || pvPManager.isInPvP(event.getPlayer())) {
                                    if (duration == 0) {
                                        event.setCancelled(true);
                                        return;
                                    }
                                    wrapper.setCooldownTicks((int) Math.ceil(remaning / 50f));
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}
