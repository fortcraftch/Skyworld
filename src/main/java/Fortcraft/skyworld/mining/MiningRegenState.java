package Fortcraft.skyworld.mining;

import java.util.Stack;

public class MiningRegenState {
    private final Stack<MiningDrop> history = new Stack<>();
    private long nextRegenTime; // Timestamp en milisegundos

    public MiningRegenState() {}

    public void pushHistory(MiningDrop drop) {
        history.push(drop);
    }

    public MiningDrop popHistory() {
        if (history.isEmpty()) return null;
        return history.pop();
    }

    public MiningDrop peekHistory() {
        if (history.isEmpty()) return null;
        return history.peek();
    }

    public boolean hasHistory() {
        return !history.isEmpty();
    }

    public long getNextRegenTime() {
        return nextRegenTime;
    }

    public void setNextRegenTime(long time) {
        this.nextRegenTime = time;
    }
}