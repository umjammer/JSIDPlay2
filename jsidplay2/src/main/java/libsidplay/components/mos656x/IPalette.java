package libsidplay.components.mos656x;

public interface IPalette {

	void setGamma(float gamma);

	void setContrast(float contrast);

	void setBrightness(float brightness);

	void setLuminanceC(float luminanceC);

	void setTint(float tint);

	void setPhaseShift(float phaseShift);

	void setOffset(float offset);

	void setSaturation(float saturation);

	void setDotCreep(float dotCreep);

}
