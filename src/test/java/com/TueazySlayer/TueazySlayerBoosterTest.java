package com.TueazySlayer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TueazySlayerBoosterTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TueazySlayerBoosterPlugin.class);
		RuneLite.main(args);
	}
}