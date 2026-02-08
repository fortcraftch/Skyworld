package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.data.PlayerData;
import org.bukkit.entity.Player;

public class EconomyManager implements Manager {

    @Override
    public void load() {}
    @Override
    public void unload() {}

    private PlayerData getData(Player p) {
        return Skyworld.getInstance().getManagerHandler().getDataManager().getPlayerData(p.getUniqueId());
    }

    public double getBalance(Player player) {
        return getData(player).getCoins();
    }

    public void addCoins(Player player, double amount) {
        getData(player).addCoins(amount);
    }

    public boolean withdraw(Player player, double amount) {
        return getData(player).removeCoins(amount);
    }
}