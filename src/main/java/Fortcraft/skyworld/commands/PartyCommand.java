package Fortcraft.skyworld.commands;

import Fortcraft.skyworld.Skyworld;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PartyCommand {

    private static PartyCommand instance;

    public static PartyCommand getInstance() {
        if(instance == null) instance = new PartyCommand();
        return instance;
    }

    private PartyCommand() {

    }

    public LiteralCommandNode<CommandSourceStack> addCommands() {

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("party").requires(this::check);

        //root.then(Commands.literal("gui").executes(this::guiPartyExecutor)); TODO: Por hacer

        root.then(Commands.literal("create").then(
                Commands.argument("Name", StringArgumentType.word()).
                        executes(this::createPartyExecutor)));

        root.then(Commands.literal("leave").executes(this::leavePartyExecutor));

        root.then(Commands.literal("setOwner").then(
                Commands.argument("newOwner", StringArgumentType.word()).
                        suggests(this::getOnlinePlayer).
                                executes(this::setOwnerExecutor)));

        root.then(Commands.literal("invite").then(
                Commands.argument("player", StringArgumentType.word()).
                        suggests(this::getOnlinePlayer).
                                executes(this::inviteExecutor)));

        return root.build();
    }

    private int createPartyExecutor(CommandContext<CommandSourceStack> cssc) {

        Player p = (Player) cssc.getSource().getExecutor();
        String name = cssc.getArgument("Name", String.class);

        assert p != null;

        Skyworld.getInstance().getManagerHandler().getPartyManager().createParty(p, name);
        return Command.SINGLE_SUCCESS;
    }

    private int leavePartyExecutor(CommandContext<CommandSourceStack> cssc) {

        Player p = (Player) cssc.getSource().getExecutor();
        assert p != null;

        Skyworld.getInstance().getManagerHandler().getPartyManager().leaveParty(p, false);

        return Command.SINGLE_SUCCESS;
    }

    private int setOwnerExecutor(CommandContext<CommandSourceStack> cssc) {

        Player p = (Player) cssc.getSource().getExecutor();
        String newOwner = cssc.getArgument("newOwner", String.class);

        assert p != null;

        Player newPlayer = Bukkit.getPlayer(newOwner);
        assert newPlayer != null;

        Skyworld.getInstance().getManagerHandler().getPartyManager().setOwner(p, newPlayer);

        return Command.SINGLE_SUCCESS;
    }

    private int inviteExecutor(CommandContext<CommandSourceStack> cssc) {

        Player p = (Player) cssc.getSource().getExecutor();
        String newOwner = cssc.getArgument("player", String.class);

        assert p != null;

        Player newPlayer = Bukkit.getPlayer(newOwner);
        assert newPlayer != null;

        Skyworld.getInstance().getManagerHandler().getPartyManager().inviteNewMember(p, newPlayer);

        return Command.SINGLE_SUCCESS;
    }

    private int guiPartyExecutor(CommandContext<CommandSourceStack> cssc) {

        Player p = (Player) cssc.getSource().getExecutor();
        assert p != null;

        p.sendMessage("test");
        return Command.SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> getOnlinePlayer(final CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder) {

        Bukkit.getOnlinePlayers().forEach(p -> {
            if(!p.isInvisible() && !p.getGameMode().equals(GameMode.SPECTATOR)) builder.suggest(p.getName());
        });

        return builder.buildFuture();
    }

    private boolean check(CommandSourceStack ctx) {
        if(!(ctx.getExecutor() instanceof Player p)) return false;
        return p.hasPermission("skyworld.party");
    }
}
