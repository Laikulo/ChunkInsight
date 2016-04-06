package guru.haun.chunkInsight.commands;

import com.google.common.collect.ImmutableSetMultimap;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.*;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by KJ4IPS on 4/4/2016.
 * Lists specifics about a dimension's chunks
 */
public class CommandChunkDim extends CommandBase{

    private static final ChatStyle keysStyle = new ChatStyle().setColor(EnumChatFormatting.GOLD).setItalic(true);
    private static final ChatStyle valueStyle = new ChatStyle().setColor(EnumChatFormatting.AQUA).setItalic(false);

    @Override
    public String getCommandName() {
        return "cdim";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/cdim <dimension>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1) {
            sender.addChatMessage(
                    new ChatComponentText(this.getCommandUsage(sender)).setChatStyle(
                            new ChatStyle().setColor(EnumChatFormatting.RED))
            );
            return;
        }
        int worldId = parseInt(args[0]);
        if (!DimensionManager.isDimensionRegistered(worldId)) {
            sender.addChatMessage(new ChatComponentText(String.format("Dim #%d does not exist or is not loaded!", worldId))
                    .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }
        WorldServer worldServer = DimensionManager.getWorld(parseInt(args[0]));

        sender.addChatMessage(
                new ChatComponentText(
                        String.format("Chunk Statistics for Dimension #%d:%s", worldId,
                                worldServer.getWorldInfo().getWorldName()
                        )
                ).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE))
        );

        ImmutableSetMultimap<ChunkCoordIntPair, ForgeChunkManager.Ticket> forcedChunks = ForgeChunkManager.getPersistentChunksFor(worldServer);

        Set<ChunkCoordIntPair> uniqueChunks = new HashSet<ChunkCoordIntPair>();
        uniqueChunks.addAll(forcedChunks.keys());

        Set<ForgeChunkManager.Ticket> uniqueTickets = new HashSet<ForgeChunkManager.Ticket>();
        uniqueTickets.addAll(forcedChunks.values());

        int playerCount = 0;
        int entityCount = 0;
        int normalCount = 0;

        for(ForgeChunkManager.Ticket ticket : uniqueTickets){
            if(ticket.isPlayerTicket()) playerCount++;
            else if(ticket.getType() == ForgeChunkManager.Type.ENTITY) entityCount++;
            else normalCount++;
        }

        Class chunkProvClass = ChunkProviderServer.class;
        Field droppedChunkField = chunkProvClass.getDeclaredField("droppedChunksSet");
        droppedChunkField.setAccessible(true);
        //SetdroppedChunkField.get(worldServer.theChunkProviderServer);


        sender.addChatMessage(kvComponent("Loaded Chunks: ",    worldServer.theChunkProviderServer.getLoadedChunkCount()));
        sender.addChatMessage(kvComponent("Chunk Tickets: ",    uniqueTickets.size()));
        sender.addChatMessage(kvComponent("Ticketed Chunks: ",  uniqueChunks.size()));
        sender.addChatMessage(kvComponent("Player Tickets: ",   playerCount));
        sender.addChatMessage(kvComponent("Entity Tickets: ",   entityCount));
        sender.addChatMessage(kvComponent("Normal Tickets: ",   normalCount));
        sender.addChatMessage(kvComponent("World Drop Queue: ", ));


    }

    private IChatComponent kvComponent(String key, int value){
        return new ChatComponentText(key).setChatStyle(keysStyle).appendSibling(
                new ChatComponentText(String.valueOf(value)).setChatStyle(valueStyle)
        );
    }



}

