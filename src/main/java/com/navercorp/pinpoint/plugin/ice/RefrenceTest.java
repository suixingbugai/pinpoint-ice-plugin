package com.navercorp.pinpoint.plugin.ice;

import java.util.HashMap;
import java.util.Map;

public class RefrenceTest {
	
	void before(Object[] args){
		args[2] = new HashMap<>();
		args[0] = "a1";
		after(args);
	}

	void doSomething(String a, String b, Map c, boolean d){
		before(new Object[]{a,b,c,d});
		System.out.println("do :"+a);
		System.out.println("do :"+b);
		System.out.println("do :"+c);
		System.out.println("do :"+d);
	}
	void after(Object[] args){
		for(Object object:args)
			System.out.println("after :"+object.toString());
	}
	
	public static void main(String[] args) {
		RefrenceTest ref = new RefrenceTest();
		final Object[] d = new Object[]{"a","b",null,true};
		ref.doSomething(d[0].toString(), d[1].toString(), d[2]==null?null:(Map)d[2], (boolean)d[3]);
		ref.after(d);
	}
}
