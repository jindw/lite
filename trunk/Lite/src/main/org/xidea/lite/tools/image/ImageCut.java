package org.xidea.lite.tools.image;


public class ImageCut {
	public int[] searchInnerBorder(int[] data,final int width,final int height){
		return null;
	}
	public int[] seachOuterBorder(int[] data,final int width,final int height){
		boolean repeatX = isRepeat(data,0,1,width);
		boolean repeatY = isRepeat(data,0,width,height);
		int x0=0,y0=0,x1=0,y1=0;
		//x0,x1
//		if(repeatX){//=
//			int[] line0 = getLine(data,0,1,width);
//			
//		}else
		{
			int[] col0 = getLine(data,0,width,height);
			x0 = searchMargin(col0, data, 0, width, 1);
//			System.out.println("x0:"+x0);
			x1 = searchMargin(col0, data, data.length-1, -width, -1);
//			System.out.println("x1:"+x1);
		}
//		if(repeatY){//||
//			
//		}else
		{
			int[] line0 = getLine(data,0,1,width);
			y0 = searchMargin(line0, data, 0, 1, width);
//			System.out.println("y0:"+y0);
			y1 = searchMargin(line0, data, data.length-1, -1, -width);
//			System.out.println("y1:"+y1);
		}
//		System.out.println(x1);
//		System.out.println(y1);
		return new int[]{x0,y0,width-x1,height-y1};
//		searchMargin(line0,data, width);
	}
	private int searchMargin( int[] line0, int[] data, int begin,int step,int lineStep) {
		int margin = 0;
//		System.out.println(begin);
		if(step>0){
			while(begin<data.length){
				if(!lineEqual(line0,data,begin,step)){
					return  margin;
				}
				margin++;
				begin+=lineStep;
			}
		}else{
			while(begin>0){
//				System.out.println("b0:"+begin);
				if(!lineEqual(line0,data,begin,step)){
					return  margin;
				}
				margin++;
				begin+=lineStep;
			}
		}
		
		return -1;
	}
	private int[] getLine(int[] data, int begin, int step,int width) {
		int[]rtv = new int[width];
		for(int i =0;i<width;i++){
			rtv[i] = data[begin];
			begin+=step;
		}
		return rtv;
	}

	private boolean lineEqual(int[] line,int[] data, int begin, int step) {
		if(step <0){
			begin =  begin + step * (line.length-1);
			step = -step;
		}
//		System.out.println("b:"+begin+",step:"+step);
		for(int i =0;i<line.length;i++){
//			System.out.println(i);
			if(!same(line[i],data[begin])){
//				System.out.println(i);
				return false;
			}
			begin+=step;
		}
		return true;
	}

	private boolean isRepeat(int[] data, int begin, int step, int count) {
		int color = data[begin];
		for (int i = 1; i < count; i++) {
			begin+=step;
			if(!same(color ,data[begin])){
				return false;
			}
		}
		return true;
	}

	private boolean same(int color,int color2) {
		return color == color2;
	}

}
