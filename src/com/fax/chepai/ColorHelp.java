package com.fax.chepai;

import android.graphics.Color;

public class ColorHelp {
	public static boolean isWhite(int red,int green,int blue){
		if(red>140&&green>150&&blue>160) return true;
//		if(red>160&&green>140&&blue>150) return true;
//		if(red>160&&green>160&&blue>150) return true;
		return false;
	}
	public static boolean isWhite(int color){
		return isWhite(Color.red(color),Color.green(color),Color.blue(color));
	}
	public static boolean isBlue(int red,int green,int blue){
		if(blue>70&&red<blue/2&&green<blue/2) return true;
//		if(blue>95&&red<blue/2&&green<blue/2) return true;
		if(blue>130&&red<blue*4/5&&green<blue*4/5&&red+green<blue*4/3) return true;
		return false;
	}
	public static boolean isBlue(int color){
		return isBlue(Color.red(color),Color.green(color),Color.blue(color));
	}
	public static boolean isBlack(int red,int green,int blue){
		if(red<50&&green<50&&blue<100) return true;
		if(red<50&&green<100&&blue<50) return true;
		if(red<100&&green<50&&blue<50) return true;
		return false;
	}
	public static boolean isBlack(int color){
		return isBlack(Color.red(color),Color.green(color),Color.blue(color));
	}
}
