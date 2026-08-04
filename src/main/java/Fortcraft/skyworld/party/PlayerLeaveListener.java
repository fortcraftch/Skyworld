package Fortcraft.skyworld.party;

import Fortcraft.skyworld.Skyworld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLeaveListener implements Listener {

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Skyworld.getInstance().getManagerHandler().getPartyManager().leaveParty(event.getPlayer(), true);
    }
}
