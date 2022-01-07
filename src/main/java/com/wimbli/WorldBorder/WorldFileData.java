package com.wimbli.WorldBorder;

import java.io.*;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.World;

// image output stuff, for debugging method at bottom of this file
import java.awt.*;
import java.awt.image.*;
import java.util.UUID;
import javax.imageio.*;


// by the way, this region file handler was created based on the divulged region file format: http://mojang.com/2011/02/16/minecraft-save-file-format-in-beta-1-3/

public class WorldFileData
{
	private transient World world;
	private transient File regionFolder = null;
	private transient File[] regionFiles = null;
	private transient UUID notifyPlayerUuid = null;

	// Use this static method to create a new instance of this class. If null is returned, there was a problem so any process relying on this should be cancelled.
	public static WorldFileData create(World world, Player notifyPlayer)
	{
		WorldFileData newData = new WorldFileData(world, notifyPlayer);

		newData.regionFolder = new File(newData.world.getWorldFolder(), "region");
		if (!newData.regionFolder.exists() || !newData.regionFolder.isDirectory())
		{
			// check for region folder inside a DIM* folder (DIM-1 for nether, DIM1 for end, DIMwhatever for custom world types)
			File[] possibleDimFolders = newData.world.getWorldFolder().listFiles(new DimFolderFileFilter());
			for (File possibleDimFolder : possibleDimFolders)
			{
				File possible = new File(newData.world.getWorldFolder(), possibleDimFolder.getName() + File.separator + "region");
				if (possible.exists() && possible.isDirectory())
				{
					newData.regionFolder = possible;
					break;
				}
			}
			if (!newData.regionFolder.exists() || !newData.regionFolder.isDirectory())
			{
				newData.sendMessage("Could not validate folder for world's region files. Looked in "+newData.world.getWorldFolder().getPath()+" for valid DIM* folder with a region folder in it.");
				return null;
			}
		}

		// Accepted region file formats: MCR is from late beta versions through 1.1, MCA is from 1.2+
		newData.regionFiles = newData.regionFolder.listFiles(new ExtFileFilter(".MCA"));
		if (newData.regionFiles == null || newData.regionFiles.length == 0)
		{
			newData.regionFiles = newData.regionFolder.listFiles(new ExtFileFilter(".MCR"));
			if (newData.regionFiles == null || newData.regionFiles.length == 0)
			{
				newData.sendMessage("Could not find any region files. Looked in: "+newData.regionFolder.getPath());
				return null;
			}
		}

		return newData;
	}

	// the constructor is private; use create() method above to create an instance of this class.
	private WorldFileData(World world, Player notifyPlayer)
	{
		this.world = world;
		if (notifyPlayer != null) this.notifyPlayerUuid = notifyPlayer.getUniqueId();
	}


	// number of region files this world has
	public int regionFileCount()
	{
		return regionFiles.length;
	}

	// folder where world's region files are located
	public File regionFolder()
	{
		return regionFolder;
	}

	// return entire list of region files
	public File[] regionFiles()
	{
		return regionFiles.clone();
	}

	// return a region file by index
	public File regionFile(int index)
	{
		if (regionFiles.length < index)
			return null;
		return regionFiles[index];
	}

	// get the X and Z world coordinates of the region from the filename
	public CoordXZ regionFileCoordinates(int index)
	{
		File regionFile = this.regionFile(index);
		String[] coords = regionFile.getName().split("\\.");
		int x, z;
		try
		{
			x = Integer.parseInt(coords[1]);
			z = Integer.parseInt(coords[2]);
			return new CoordXZ (x, z);
		}
		catch(Exception ex)
		{
			sendMessage("Error! Region file found with abnormal name: "+regionFile.getName());
			return null;
		}
	}




	// send a message to the server console/log and possibly to an in-game player
	private void sendMessage(String text)
	{
		Config.log("[WorldData] " + text);
		if (notifyPlayerUuid != null) {
			Player player = Bukkit.getPlayer(notifyPlayerUuid);
			if (player != null) player.sendMessage("[WorldData] " + text);
		}
	}

	// file filter used for region files
	private static class ExtFileFilter implements FileFilter
	{
		String ext;
		public ExtFileFilter(String extension)
		{
			this.ext = extension.toLowerCase();
		}

		@Override
		public boolean accept(File file)
		{
			return (
				   file.exists()
				&& file.isFile()
				&& file.getName().toLowerCase().endsWith(ext)
				);
		}
	}

	// file filter used for DIM* folders (for nether, End, and custom world types)
	private static class DimFolderFileFilter implements FileFilter
	{
		@Override
		public boolean accept(File file)
		{
			return (
				   file.exists()
				&& file.isDirectory()
				&& file.getName().toLowerCase().startsWith("dim")
				);
		}
	}


	// crude chunk map PNG image output, for debugging
	private void testImage(CoordXZ region, List<Boolean> data) {
		int width = 32;
		int height = 32;
		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = bi.createGraphics();
		int current = 0;
		g2.setColor(Color.BLACK);

		for (int x = 0; x < 32; x++)
		{
			for (int z = 0; z < 32; z++)
			{
				if (data.get(current).booleanValue())
					g2.fillRect(x,z, x+1, z+1);
				current++;
			}
		}

		File f = new File("region_"+region.x+"_"+region.z+"_.png");
		Config.log(f.getAbsolutePath());
		try {
			// png is an image format (like gif or jpg)
			ImageIO.write(bi, "png", f);
		} catch (IOException ex) {
			Config.log("[SEVERE]" + ex.getLocalizedMessage());
		}
	}
}
