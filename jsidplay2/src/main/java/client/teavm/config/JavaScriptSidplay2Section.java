package client.teavm.config;

import java.io.File;

import client.teavm.IConfigResolver;
import libsidplay.config.ISidPlay2Section;

public final class JavaScriptSidplay2Section implements ISidPlay2Section {

	private IConfigResolver resolver;

	public JavaScriptSidplay2Section(IConfigResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public void setVersion(int version) {
	}

	@Override
	public void setTurboTape(boolean turboTape) {
	}

	@Override
	public void setTmpDir(File tmpDir) {
	}

	@Override
	public void setTint(float tint) {
	}

	@Override
	public void setStartTime(double startTime) {
	}

	@Override
	public void setSingle(boolean singleSong) {
	}

	@Override
	public void setSaturation(float saturation) {
	}

	@Override
	public void setPhaseShift(float phaseShift) {
	}

	@Override
	public void setPalEmulation(boolean palEmulation) {
	}

	@Override
	public void setOffset(float offset) {
	}

	@Override
	public void setLoop(boolean loop) {
	}

	@Override
	public void setLastDirectory(File lastDir) {
	}

	@Override
	public void setHvsc(File hvsc) {
	}

	@Override
	public void setGamma(float gamma) {
	}

	@Override
	public void setFadeOutTime(double fadeOutTime) {
	}

	@Override
	public void setFadeInTime(double fadeInTime) {
	}

	@Override
	public void setEnableDatabase(boolean enable) {
	}

	@Override
	public void setDefaultPlayLength(double playLength) {
	}

	@Override
	public void setContrast(float contrast) {
	}

	@Override
	public void setBrightness(float brightness) {
	}

	@Override
	public void setBlur(float blur) {
	}

	@Override
	public void setBleed(float bleed) {
	}

	@Override
	public boolean isTurboTape() {
		return false;
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public boolean isPalEmulation() {
		return resolver.isPalEmulation();
	}

	@Override
	public boolean isLoop() {
		return false;
	}

	@Override
	public boolean isEnableDatabase() {
		return false;
	}

	@Override
	public int getVersion() {
		return JavaScriptConfig.REQUIRED_CONFIG_VERSION;
	}

	@Override
	public File getTmpDir() {
		return null;
	}

	@Override
	public float getTint() {
		return 0;
	}

	@Override
	public double getStartTime() {
		return 0;
	}

	@Override
	public float getSaturation() {
		return 0;
	}

	@Override
	public float getPhaseShift() {
		return -15;
	}

	@Override
	public float getOffset() {
		return 1;
	}

	@Override
	public File getLastDirectory() {
		return null;
	}

	@Override
	public File getHvsc() {
		return null;
	}

	@Override
	public float getGamma() {
		return 2;
	}

	@Override
	public double getFadeOutTime() {
		return 0;
	}

	@Override
	public double getFadeInTime() {
		return 0;
	}

	@Override
	public double getDefaultPlayLength() {
		return 300;
	}

	@Override
	public float getContrast() {
		return 1;
	}

	@Override
	public float getBrightness() {
		return 0;
	}

	@Override
	public float getBlur() {
		return 0.5f;
	}

	@Override
	public float getBleed() {
		return 0.5f;
	}
}