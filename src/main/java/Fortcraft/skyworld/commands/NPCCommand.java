package Fortcraft.skyworld.commands;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.npcs.NPCMenuType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class NPCCommand {

    private static NPCCommand instance;

    public static NPCCommand getInstance() {
        if(instance == null) instance = new NPCCommand();
        return instance;
    }

    private NPCCommand() {

    }

    public LiteralCommandNode<CommandSourceStack> addCommands() {

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("npc").requires(this::check);

        root.then(Commands.literal("create").
                then(Commands.argument("Name", StringArgumentType.word()).
                        then(Commands.argument("EntityType", ArgumentTypes.resource(RegistryKey.ENTITY_TYPE)).
                                then(Commands.argument("Menu", StringArgumentType.word()).suggests(this::createNPCSuggests).
                                        executes(this::createNPCExecutor)
        ))));

        root.then(Commands.literal("remove").
                then(Commands.argument("Id", StringArgumentType.word()).
                        executes(this::removeNPCExecutor)
        ));


        return root.build();
    }

    private int createNPCExecutor(CommandContext<CommandSourceStack> cssc) {

        Player p = (Player) cssc.getSource().getExecutor();
        String name = cssc.getArgument("Name", String.class);
        EntityType type = cssc.getArgument("EntityType", EntityType.class);
        String menu = cssc.getArgument("Menu", String.class);

        assert p != null;

        Skyworld.getInstance().getManagerHandler().getNpcManager().createNPC(name, type, p.getLocation(), NPCMenuType.valueOf(menu));

        return Command.SINGLE_SUCCESS;
    }

    private int removeNPCExecutor(CommandContext<CommandSourceStack> cssc) {

        String id = cssc.getArgument("Id", String.class);

        Skyworld.getInstance().getManagerHandler().getNpcManager().deleteNPC(id);

        return Command.SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> createNPCSuggests(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {

        for (NPCMenuType type : NPCMenuType.values()) {
            builder.suggest(type.toString());
        }

        return builder.buildFuture();
    }

    private boolean check(CommandSourceStack ctx) {
        if(!(ctx.getExecutor() instanceof Player p)) return false;
        return p.hasPermission("skyworld.admin.npc");
    }
}
