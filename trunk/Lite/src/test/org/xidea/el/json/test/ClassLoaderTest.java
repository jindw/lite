package org.xidea.el.json.test;

public class ClassLoaderTest {
	static class A{
		static{
			System.out.println(111);
		}
	}
	public static void main(String[] args) throws Exception{
		System.out.println(Class.forName(A.class.getName()));
		A.class.getFields();
		System.out.println(A.class);
	}

}
