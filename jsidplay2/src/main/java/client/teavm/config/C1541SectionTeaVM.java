package client.teavm.config;

import client.teavm.IConfigResolverTeaVM;
import libsidplay.components.c1541.FloppyType;
import libsidplay.config.IC1541Section;

public final class C1541SectionTeaVM implements IC1541Section {

	private boolean driveOn;

	private IConfigResolverTeaVM resolver;

	public C1541SectionTeaVM(IConfigResolverTeaVM resolver) {
		this.resolver = resolver;
	}

	@Override
	public void setRamExpansionEnabled4(boolean on) {
	}

	@Override
	public void setRamExpansionEnabled3(boolean on) {
	}

	@Override
	public void setRamExpansionEnabled2(boolean on) {
	}

	@Override
	public void setRamExpansionEnabled1(boolean on) {
	}

	@Override
	public void setRamExpansionEnabled0(boolean on) {
	}

	@Override
	public void setParallelCable(boolean on) {
	}

	@Override
	public void setJiffyDosInstalled(boolean on) {
	}

	@Override
	public void setFloppyType(FloppyType floppyType) {
	}

	@Override
	public void setDriveOn(boolean on) {
		driveOn = on;
	}

	@Override
	public boolean isRamExpansionEnabled4() {
		return false;
	}

	@Override
	public boolean isRamExpansionEnabled3() {
		return false;
	}

	@Override
	public boolean isRamExpansionEnabled2() {
		return false;
	}

	@Override
	public boolean isRamExpansionEnabled1() {
		return false;
	}

	@Override
	public boolean isRamExpansionEnabled0() {
		return false;
	}

	@Override
	public boolean isParallelCable() {
		return false;
	}

	@Override
	public boolean isJiffyDosInstalled() {
		return resolver.isJiffyDosInstalled();
	}

	@Override
	public boolean isDriveOn() {
		return driveOn;
	}

	@Override
	public FloppyType getFloppyType() {
		return FloppyType.C1541;
	}
}