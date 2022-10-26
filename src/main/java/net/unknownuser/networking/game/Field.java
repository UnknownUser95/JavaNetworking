package net.unknownuser.networking.game;

import java.util.function.*;

import org.eclipse.swt.graphics.*;

public class Field {
	private String symbol = " ";
	private int red = 255;
	private int green = 255;
	private int blue = 255;
	
	public Field(int x, int y, String symbol) {
		super();
		this.symbol = symbol;
	}
	
	public Field(int x, int y) {
		super();
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	
	public RGB getColour() {
		return new RGB(red, green, blue);
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
		return "Field [symbol=" + symbol + ", red=" + red + ", green=" + green + ", blue=" + blue + "]";
	}
}
