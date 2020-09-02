package com.yc.util;

import java.util.Random;

import javax.swing.plaf.synth.SynthScrollBarUI;

public class StringUtil {
/**
 * 字符串非空判断
 * @param str
 * @return
 */
	public static boolean isNotNull(String str){
		if(null==str ||"".equals(str)){
			return false;
		}
		return true;
	}
	/**
	 * 对象转字符串
	 * @param obj
	 * @return
	 */
	public static String objToString(Object obj){
		if(null==obj){
			return null;
		}
		return obj.toString();
	}
	//生成订单编号
	public static String genOrderId(){
		return System.currentTimeMillis()+""+new Random().nextInt(1000);
	}
}
