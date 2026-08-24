/*
 * This file is part of Dynamic Surroundings, licensed under the MIT License (MIT).
 *
 * Copyright (c) OreCruncher
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.blockartistry.mod.DynSurround;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Modeled after the BuildCraft version check system.
public final class VersionCheck implements Runnable {

	private static final String REMOTE_VERSION_FILE = "https://raw.githubusercontent.com/mist475/DynamicSurroundings/master/versions.txt";
	private static final int VERSION_CHECK_RETRIES = 3;
	private static final int VERSION_CHECK_INTERVAL = 10000;

	public enum UpdateStatus {
		UNKNOWN, CURRENT, OUTDATED, COMM_ERROR
	}

	public static final SoftwareVersion modVersion = new SoftwareVersion(Module.VERSION);
	public static SoftwareVersion currentVersion = new SoftwareVersion();
	public static UpdateStatus status = UpdateStatus.UNKNOWN;

	private static final String mcVersion = Loader.instance().getMinecraftModContainer().getVersion();

    public static VersionCheck instance;

	public static class SoftwareVersion implements Comparable<SoftwareVersion> {

		public final int major;
		public final int minor;
		public final int revision;
		public final int patch;
		public final boolean isAlpha;
		public final boolean isBeta;

		public SoftwareVersion() {
			this.major = 0;
			this.minor = 0;
			this.revision = 0;
			this.patch = 0;
			this.isAlpha = false;
			this.isBeta = false;
		}

        public SoftwareVersion(String versionString) {

            if (StringUtils.isBlank(versionString)) {
                this.major = 0;
                this.minor = 0;
                this.revision = 0;
                this.patch = 0;
                this.isAlpha = false;
                this.isBeta = false;
                return;
            }

            versionString = versionString.trim();

            // Old git tags start with v
            if (versionString.startsWith("v")) {
                versionString = versionString.substring(1);
            }

            // Dirty builds = uncommitted git changes
            versionString = StringUtils.remove(versionString, ".dirty");
            versionString = StringUtils.remove(versionString, "-dirty");

            this.isAlpha = StringUtils.containsIgnoreCase(versionString, "ALPHA");
            this.isBeta = StringUtils.containsIgnoreCase(versionString, "BETA");

            versionString = versionString.replaceAll("(?i)ALPHA", "");
            versionString = versionString.replaceAll("(?i)BETA", "");

            /*
             * Accept versions such as:
             * 1.2.3
             * 1.2.3.4
             * v1.2.3
             * 1.2.3-41-g10064b2
             *
             * Git-generated development versions without a numeric version,
             * such as a0adc40-master+a0adc40aaf-dirty, become 0.0.0.
             */
            final Matcher matcher = Pattern.compile(
                "^(\\d+)\\.(\\d+)\\.(\\d+)(?:\\.(\\d+))?"
            ).matcher(versionString);

            if (!matcher.find()) {
                this.major = 0;
                this.minor = 0;
                this.revision = 0;
                this.patch = 0;
                return;
            }

            this.major = Integer.parseInt(matcher.group(1));
            this.minor = Integer.parseInt(matcher.group(2));
            this.revision = Integer.parseInt(matcher.group(3));
            this.patch = matcher.group(4) != null
                ? Integer.parseInt(matcher.group(4))
                : 0;
        }
		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append(this.major).append('.').append(this.minor).append('.').append(this.revision);
			if (this.patch != 0)
				builder.append('.').append(this.patch);
			if (this.isAlpha)
				builder.append("ALPHA");
			if (this.isBeta)
				builder.append("BETA");
			return builder.toString();
		}

		@Override
		public int compareTo(final SoftwareVersion obj) {

			if (this.major != obj.major)
				return this.major - obj.major;

			if (this.minor != obj.minor)
				return this.minor - obj.minor;

			if (this.revision != obj.revision)
				return this.revision - obj.revision;

			return this.patch - obj.patch;
		}
	}

	private VersionCheck() {
	}

	public static void register() {

		if (ModOptions.enableVersionChecking) {
			VersionCheck.instance = new VersionCheck();
            MinecraftForge.EVENT_BUS.register(instance);
			new Thread(instance).start();
		}
	}

	@SubscribeEvent
	public void playerLogin(final EntityJoinWorldEvent event) {

		if (event.entity instanceof EntityPlayer player) {
			if (status == UpdateStatus.OUTDATED) {
				final String msg = StatCollector.translateToLocalFormatted("msg.NewVersionAvailable.dsurround",
						Module.MOD_NAME, currentVersion);
				final IChatComponent component = IChatComponent.Serializer.func_150699_a(msg);
				player.addChatMessage(component);
			}
            MinecraftForge.EVENT_BUS.unregister(VersionCheck.instance);
		}
	}

	private static void versionCheck() {
		try {

            final URL url = new URL(REMOTE_VERSION_FILE);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			if (conn == null) {
				throw new NullPointerException();
			}

			final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			String line;
			while ((line = reader.readLine()) != null) {
				final String[] tokens = line.split(":");
				if (mcVersion.matches(tokens[0])) {
					currentVersion = new SoftwareVersion(tokens[1]);
					break;
				}
			}

			status = UpdateStatus.CURRENT;
			if (modVersion.compareTo(currentVersion) < 0)
				status = UpdateStatus.OUTDATED;

			conn.disconnect();
			reader.close();

		} catch (final Exception e) {
			ModLog.warn("Unable to read remote version data", e);
			status = UpdateStatus.COMM_ERROR;
		}
	}

	@Override
	public void run() {

		int count = 0;

		ModLog.info("Checking for newer mod version");

		try {

			do {
				if (count > 0) {
					ModLog.info("Awaiting attempt %d", count);
					Thread.sleep(VERSION_CHECK_INTERVAL);
				}
				versionCheck();
				count++;
			} while (count < VERSION_CHECK_RETRIES && status == UpdateStatus.COMM_ERROR);

		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

        switch (status) {
            case COMM_ERROR -> ModLog.warn("Version check failed");
            case CURRENT ->
                ModLog.info("Dynamic Surroundings version [%s] is the same or newer than the current version [%s]",
                            modVersion, currentVersion);
            case OUTDATED -> ModLog.warn("Using outdated version [" + modVersion + "] for Minecraft " + mcVersion
                                             + ". Consider updating to " + currentVersion + ".");
            case UNKNOWN -> ModLog.warn("Unknown version check status!");
            default -> {
            }
        }
	}
}
