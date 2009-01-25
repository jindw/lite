package org.xidea.lite.test;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import org.xidea.el.ExpressionImpl;
import org.xidea.el.json.JSONEncoder;

public class TemplateData implements Externalizable {
	private String[] files;
	
	private List<Object> data;

	public TemplateData(String[] files,List<Object> data) {
		this.files = files;
		this.data = data;
	}
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		ExpressionImpl el = new ExpressionImpl(in.readUTF());
		List<?> result = (List<?>) el.evaluate(null);
		@SuppressWarnings("unchecked")
		List<String> files = (List<String>) result.get(0);
		this.files = files.toArray(new String[files.size()]);
//		@SuppressWarnings("unchecked")
//		List<Object> data = (List<Object>) result.get(1);
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		String result = JSONEncoder.encode(new Object[]{files,data});
		out.writeUTF("\n"+result+"\n//");
	}
	
	public static void main(String[] args) throws IOException{
		System.out.println();
		
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(buf);

		out.writeUTF("//123");
		out.writeObject(new TemplateData(new String[]{"ddd"},Arrays.asList((Object)"1")));
		//out.writeObject(new TemplateData(new String[]{"ddd","23"},Arrays.asList((Object)"1")));
//		out.writeUTF("\r\n//123");
//		out.writeObject(new Object[]{1,"2323",'3'});
//		out.writeUTF("<4343金大丄1�7");
//		out.writeUTF("<4343金÷人\r\n大为>");
		out.flush();
		System.out.println(buf.toString("utf-8"));
	}

}
