package dev.kgriffon.capes;

import dev.kgriffon.capes.command.CapeCommand;
import dev.kgriffon.capes.util.CapeManager;
import net.fabricmc.api.ClientModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoreCapes implements ClientModInitializer {
	public static final String MOD_ID = "more-capes";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {

        CapeManager.reload();
        CapeCommand.register();

        LOGGER.info("Thank you for using MoreCapes ♥");
	}
}