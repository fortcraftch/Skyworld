package Fortcraft.skyworld.party;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Party {

    private static final int maxMember = 4;

    private UUID idOwner;
    private final List<UUID> idMembers = new ArrayList<>();
    private final String name;

    public Party (UUID idOwner, String name) {
        this.idOwner = idOwner;
        idMembers.add(idOwner);
        this.name = name;
    }

    public void setIdOwner(UUID idOwner) {
        this.idOwner = idOwner;
    }

    public void addMember(UUID id) {

        if(idOwner == id) return;
        idMembers.add(id);
    }

    public void removeMember(UUID id) {
        idMembers.remove(id);
    }

    public boolean isFull() {
        return maxMember == idMembers.size();
    }

    public List<UUID> getIdMembers() {
        return idMembers;
    }

    public UUID getIdOwner() {
        return idOwner;
    }

    public String getName() {
        return name;
    }
}
