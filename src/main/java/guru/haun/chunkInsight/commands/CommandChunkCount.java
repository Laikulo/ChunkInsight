package guru.haun.chunkInsight.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

/**
 * Created by KJ4IPS on 4/4/2016.
 * This lists chunks loaded for a dimension
 */
public class CommandChunkCount extends CommandBase {
    @Override
    public String getCommandName() {
        return "ccount";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ccount";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        sender.addChatMessage(new ChatComponentTranslation("cinsight.chunk.counts"));
        for(int dimID : DimensionManager.getIDs()){
            WorldServer worldServer = DimensionManager.getWorld(dimID);
            sender.addChatMessage(
                    new ChatComponentText(
                            String.format("#%d %s:%d",
                                    dimID,
                                    worldServer.getWorldInfo().getWorldName(),
                                    worldServer.theChunkProviderServer.getLoadedChunkCount())
                    )
            );
        }
    }
}
