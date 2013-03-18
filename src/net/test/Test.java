package net.test;

import java.util.ArrayList;

public class Test {
	public static ArrayList<Apple> list = new ArrayList<Apple>();
	
	public Test(){
		list.add(new Apple());
		list.add(new Apple());
		list.add(new Apple());
		
		list.clear();
		System.out.println(list.size());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Test();
		System.out.println(list.size());
	}
	
	class Apple{
		
	}

}
