package net.unknownuser.networking.game;

import java.util.function.*;

import org.eclipse.swt.graphics.*;

public class Field {
	private int red = 255;
	private int green = 255;
	private int blue = 255;
	
	public Field(int red, int green, int blue) {
		super();
		this.red = red;
		this.green = green;
		this.blue = blue;
	}
	
	public Field() {
		super();
	}

	public RGB getColour() {
		return new RGB(red, green, blue);
	}
	
	public int getRed() {
		return red;
	}

	public int getGreen() {
		return green;
	}

	public int getBlue() {
		return blue;
	}

	public void setColour(int red, int green, int blue) {
		IntPredicate is8Bit = num -> num >= 0 && num <= 255;
		if(is8Bit.test(red) && is8Bit.test(green) && is8Bit.test(blue)) {
			this.red = red;
			this.green = green;
			this.blue = blue;
		}
	}

	@Override
	public String toString() {
		return "Field [red=" + red + ", green=" + green + ", blue=" + blue + "]";
	}
}
