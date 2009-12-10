package org.xidea.el.fn;

import org.xidea.el.Invocable;
import org.xidea.el.impl.CalculaterImpl;

abstract class JSObject extends ECMA262Impl implements Invocable {
	int type;
	String name;

	@SuppressWarnings("unchecked")
	static void setup(CalculaterImpl calculater, Class<? extends JSObject> impl,
			Class... forClass) {
		try {
			String[] members = ((String) impl.getField("MEMBERS").get(null)).split("[,]");
			for (int i = 0; i < members.length; i++) {
				JSObject inv = impl.newInstance();
				inv.name = members[i];
				inv.type = i;
				for (Class type : forClass) {
					calculater.addMethod(type, members[i], inv);
				}

			}
		} catch (Exception e) {
		}

	}

	public String toString() {
		return name;
	}
}
