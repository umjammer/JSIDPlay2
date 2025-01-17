/**
 *                         C64 PRG file format support.
 *                         ----------------------------
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package libsidplay.sidtune;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import libsidutils.IOUtils;
import libsidutils.sidid.SidIdInfo;
import libsidutils.sidid.SidIdInfo.PlayerInfoSection;
import libsidutils.sidid.SidIdV2;

class Prg extends SidTune {

	private static SidIdV2 SID_ID;
	private static SidIdInfo SID_ID_INFO;

	protected int programOffset;

	protected byte[] program;

	protected static SidTune load(final String name, final byte[] dataBuf) throws SidTuneError {
		if (!IOUtils.getFilenameSuffix(name).equalsIgnoreCase(".prg") || dataBuf.length < 2) {
			throw new SidTuneError("PRG: Bad file extension expected: .prg and length > 2");
		}
		final Prg prg = new Prg();

		prg.program = dataBuf;
		prg.programOffset = 2;
		prg.info.c64dataLen = dataBuf.length - prg.programOffset;
		prg.info.loadAddr = (dataBuf[0] & 0xff) + ((dataBuf[1] & 0xff) << 8);

		prg.info.infoString.add(IOUtils.getFilenameWithoutSuffix(name));

		return prg;
	}

	@Override
	public void save(final String name) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(!name.endsWith(".prg") ? name + ".prg" : name)) {
			fos.write(program);
		}
	}

	@Override
	public Integer placeProgramInMemory(final byte[] mem) {
		final int start = info.loadAddr;
		final int end = start + info.c64dataLen;
		mem[0x2d] = (byte) (end & 0xff);
		mem[0x2e] = (byte) (end >> 8); // Variables start
		mem[0x2f] = (byte) (end & 0xff);
		mem[0x30] = (byte) (end >> 8); // Arrays start
		mem[0x31] = (byte) (end & 0xff);
		mem[0x32] = (byte) (end >> 8); // Strings start
		mem[0xac] = (byte) (start & 0xff);
		mem[0xad] = (byte) (start >> 8);
		mem[0xae] = (byte) (end & 0xff);
		mem[0xaf] = (byte) (end >> 8);

		// Copy data from cache to the correct destination.
		System.arraycopy(program, programOffset, mem, start, end - start);
		return null;
	}

	@Override
	public Integer placeProgramInMemoryTeaVM(final byte[] mem, byte[] driver) {
		final int start = info.loadAddr;
		final int end = start + info.c64dataLen;
		mem[0x2d] = (byte) (end & 0xff);
		mem[0x2e] = (byte) (end >> 8); // Variables start
		mem[0x2f] = (byte) (end & 0xff);
		mem[0x30] = (byte) (end >> 8); // Arrays start
		mem[0x31] = (byte) (end & 0xff);
		mem[0x32] = (byte) (end >> 8); // Strings start
		mem[0xac] = (byte) (start & 0xff);
		mem[0xad] = (byte) (start >> 8);
		mem[0xae] = (byte) (end & 0xff);
		mem[0xaf] = (byte) (end >> 8);

		// Copy data from cache to the correct destination.
		System.arraycopy(program, programOffset, mem, start, end - start);
		return null;
	}

	/**
	 * Identify the player IDs of a program in memory.
	 *
	 * @return the player IDs as a list
	 */
	@Override
	public Collection<String> identify() {
		if (SID_ID == null) {
			try {
				SID_ID = new SidIdV2();
				SID_ID.readconfig();
				SID_ID.setMultiScan(true);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		return SID_ID.identify(program);
	}

	/**
	 * Search player ID Info.
	 *
	 * @param playerName player to get infos for
	 * @return player infos (or null, if not found)
	 */
	@Override
	public PlayerInfoSection getPlayerInfo(String playerName) {
		if (SID_ID_INFO == null) {
			try {
				SID_ID_INFO = new SidIdInfo();
				SID_ID_INFO.readconfig();
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		return SID_ID_INFO.getPlayerInfo(playerName);
	}

	/**
	 * Calculate MD5 checksum.
	 */
	@Override
	public String getMD5Digest(MD5Method md5Method) {
		StringBuilder md5 = new StringBuilder();
		try {
			final byte[] encryptMsg = MessageDigest.getInstance("MD5").digest(program);
			for (final byte anEncryptMsg : encryptMsg) {
				md5.append(Character.forDigit((anEncryptMsg >> 4) & 0xF, 16));
				md5.append(Character.forDigit((anEncryptMsg & 0xF), 16));
			}
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		return md5.toString();
	}

	@Override
	protected long getInitDelay() {
		/* Wait for completion of a normal reset before initializing PRG/P00. */
		return RESET_INIT_DELAY;
	}

}