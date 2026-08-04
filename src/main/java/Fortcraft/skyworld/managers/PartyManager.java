package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.party.Party;
import Fortcraft.skyworld.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyManager implements Manager {

    private static final Map<UUID, Party> parties = new HashMap<>();

    @Override
    public void load() {}

    @Override
    public void unload() {}

    public void createParty(Player p, String name) {

        UUID id = p.getUniqueId();
        Party pt = getParty(id);

        if(pt == null) {
            Party party = new Party(id, name);
            parties.put(id, party);
            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:green>La party ha sido creada con éxito."));
            return;
        }

        if(pt.getIdOwner().equals(id)) {
            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>Tienes una party ya creada."));
            return;
        }

        p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>Te encuentras en una party. Sal para poder crear una."));
    }

    public void leaveParty(Player p, boolean forced) {

        UUID id = p.getUniqueId();
        Party party = getParty(id);

        if(party == null) {
            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>No estas en ninguna party."));
            return;
        }

        if(!forced) p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:green>Has salido de tu party."));

        if(party.getIdOwner().equals(id)) {

            if(party.getIdMembers().size() == 1) {

                if(!forced) p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:green>No hay mas jugadores. La party será eliminada."));
                parties.remove(id);
                return;
            }

            party.removeMember(id);
            Player newOwner = Bukkit.getPlayer(party.getIdMembers().getFirst());
            assert newOwner != null;

            if(!forced) p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:green>Se ha escogido un nuevo lider."));
            newOwner.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:green>El anterior líder ha abandona la party. Ahora eres tu el líder"));

            party.setIdOwner(newOwner.getUniqueId());
        }

        party.removeMember(id);
    }

    public void setOwner(Player p, Player newOwner) {

        UUID id = p.getUniqueId();
        Party party = getParty(id);

        if(newOwner == null) {
            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>Debe de estar online para ser el nuevo líder."));
            return;
        }

        UUID newId = newOwner.getUniqueId();

        if(party == null) {
            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>No estas en ninguna party."));
            return;
        }

        if(!party.getIdOwner().equals(id)) {
            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>No eres el líder de esta party."));
            return;
        }

        if(newId.equals(id)) {
            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>Ya eres el líder de esta party."));
            return;
        }

        if(!party.getIdMembers().contains(newId)) {
            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>El nuevo líder debe de ser un miembro de la party."));
            return;
        }

        p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>Has dejado de ser el líder."));
        newOwner.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:green>Eres el nuevo líder de esta party."));

        party.setIdOwner(newId);
    }

    public void inviteNewMember(Player p, Player newPlayer) {

        UUID id = p.getUniqueId();
        Party party = getParty(id);

        if(party == null) {
            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>No estas en ninguna party."));
            return;
        }

        if(newPlayer == null) {
            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>Debe de estar online para poder invitarlo."));
            return;
        }

        if(!party.getIdOwner().equals(id)) {
            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>No eres el líder de esta party."));
            return;
        }

        if(party.isFull()) {
            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>La party esta llena."));
            return;
        }

        if(party.getIdOwner().equals(newPlayer.getUniqueId())) {
            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>No te puedes invitar a ti mismo."));
            return;
        }

        if(party.getIdMembers().contains(newPlayer.getUniqueId())) {
            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>El jugador ya es miembro de la party."));
            return;
        }

        newPlayer.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:green>Te han invitado a la party de "+ p.getName()));
        TextComponent component = ColorUtils.setComponent("<color:yellow>[Party] ");

        TextComponent accept = Component.text("[Aceptar] ", TextColor.color(Color.GREEN.asRGB()), TextDecoration.BOLD).clickEvent(ClickEvent.callback((_) -> {

            if(canJoinParty(newPlayer, party)) return;

            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:green>"+newPlayer.getName()+" ha aceptado la invitación."));
            newPlayer.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:green>Te ha unido a la party."));

            party.getIdMembers().forEach(uuids -> {

                Player otherPlayer = Bukkit.getPlayer(uuids);
                assert otherPlayer != null;

                otherPlayer.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:green>"+newPlayer.getName()+" se ha unido a la party."));

            });

            party.addMember(newPlayer.getUniqueId());

        }, ClickCallback.Options.builder().uses(1).build()));

        component = component.append(accept);

        TextComponent deny = Component.text("[Rechazar]", TextColor.color(Color.RED.asRGB()), TextDecoration.BOLD).clickEvent(ClickEvent.callback((_) -> {

            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:green>"+newPlayer.getName()+" ha rechazado la invitación."));
            newPlayer.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>Has rechazado unirte a la party."));

        }, ClickCallback.Options.builder().uses(1).build()));

        component = component.append(deny);


        newPlayer.sendMessage(component);
    }

    private boolean canJoinParty(Player p, Party party) {

        if(party.isFull()) {
            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>La party ya esta llena."));
            return false;
        }

        if(getParty(p.getUniqueId()) != null) {
            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>Ya estas en una party."));
            return false;
        }

        if(party.getIdMembers().contains(p.getUniqueId())) {
            p.sendMessage(ColorUtils.setComponent("<color:yellow>[Party] <color:red>Ya eres miembro de la party."));
            return false;
        }

        return true;
    }

    public @Nullable Party getParty(UUID playerID) {
        return parties.values().stream().filter(par -> par.getIdMembers().contains(playerID)).findAny().orElse(null);
    }
}
