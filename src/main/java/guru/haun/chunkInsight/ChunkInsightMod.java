package guru.haun.chunkInsight;

import guru.haun.chunkInsight.commands.CommandChunkCount;
import guru.haun.chunkInsight.commands.CommandChunkDim;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * Entry point class for CInsight
 */

@Mod(modid = "cinsight", name = "Chunk Insight", acceptableRemoteVersions = "*")
public class ChunkInsightMod {

    @Mod.Instance
    private ChunkInsightMod INSTANCE;

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent startingEvent){
        startingEvent.registerServerCommand(new CommandChunkCount());
        startingEvent.registerServerCommand(new CommandChunkDim());
    }

    public ChunkInsightMod getInstance(){
        return INSTANCE;
    }
}
