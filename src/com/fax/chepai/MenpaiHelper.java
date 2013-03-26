package com.fax.chepai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.fax.chepai.ChepaiHelper.ReadInfo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.widget.Toast;

public class MenpaiHelper{
	private static final boolean checkZifu=true;
	Bitmap mybitmap;
	ArrayList<Rect> rects;
	ArrayList<OneRectInfo> rectsInfo;
	ArrayList<Bitmap> rectBitmaps=new ArrayList<Bitmap>();
//	ArrayList<ArrayList<Bitmap>> cutbitmaps;
	
	public MenpaiHelper(Bitmap bitmap){
		this.mybitmap=bitmap;
		initChepaiRect();
	}
	public void startInitValueThread(final Runnable callback){
		new Thread(){
			public void run(){
				initValues();
				if(callback!=null){
					callback.run();
				}
			}
		}
		.start();
	}
	private static final int COLOR_TYPE_BLACK=0;
	private static final int COLOR_TYPE_WHITE=1;
	private static final int COLOR_TYPE_BLUE=2;
	private static final int COLOR_TYPE_OTHER=-1;
    @SuppressLint("UseSparseArrays")
    public void initChepaiRect() {
    	int width=mybitmap.getWidth();int height=mybitmap.getHeight();
    	int wantwidth=200;
    	float wantscale=((float)wantwidth)/width;
    	int wantheight=(int) (height*wantscale);
    	Bitmap smallbitmap=Bitmap.createScaledBitmap(mybitmap, (int)wantwidth, (int)wantheight, false);
    	
    	//rightYLine储存了每一正确行的蓝白颜色分布情况
    	HashMap<Integer,ArrayList<int[]>> rightYLine=new HashMap<Integer,ArrayList<int[]>>();
    	ArrayList<Integer> yLineKeys=new ArrayList<Integer>();
    	for(int y=0;y<wantheight;y++){
    		ArrayList<int[]> getlist=getBlueWhiteLineInfo(getYLineColorType(smallbitmap, y));
    		if(getlist!=null){
    			yLineKeys.add(y);
    			rightYLine.put(y, getlist);
    		}
    	}
    	removeSingleLine(yLineKeys,rightYLine);
    	setRectsByYLineInfo(rightYLine, yLineKeys, wantscale);
    	Log.d("fax", "RectNum:"+rects.size());
    	for(Rect reRect :rects){
    		Bitmap temp=Bitmap.createBitmap(mybitmap, reRect.left, reRect.top, reRect.width(), reRect.height());
    		rectBitmaps.add(Bitmap.createScaledBitmap(temp, 200, temp.getHeight()*200/temp.getWidth(), true));
    		temp.recycle();
    	}
    }
    //得出数个满足条件的矩形范围(2个)
    public void setRectsByYLineInfo(HashMap<Integer,ArrayList<int[]>> rightYLine,ArrayList<Integer> yLineKeys,float wantscale){
    	rects=new ArrayList<Rect>();
    	for(int[] yRangeInfo:getChepaiYRange(yLineKeys, rightYLine)){
    		int rect_height=(int) (yRangeInfo[1]*1.1f);
        	int rect_top=yRangeInfo[0];
        	if(rect_height<5) continue;
        	int[] xRangeInfo=getChepaiXRange(rightYLine, rect_top, rect_height);
        	int rect_left=xRangeInfo[0];
        	int rect_width=xRangeInfo[1];
        	
        	if(rect_height/wantscale<10||rect_width/wantscale<40) continue  ;
        	if((float)rect_width/rect_height<2) continue  ;
        	if((float)rect_width/rect_height>8) continue  ;
        	Rect reRect=new Rect((int) (rect_left/wantscale),(int) (rect_top/wantscale), (int)((rect_width+rect_left-1)/wantscale), (int)((rect_height+rect_top-1)/wantscale));
        	
        	if(rects.size()==2){
        		if(rects.get(0).height()>rects.get(1).height()){
        			rects.add(rects.get(0));
        			rects.remove(0);
        		}
        		if(reRect.height()>rects.get(0).height()){
        			rects.set(0, reRect);
        		}
        	}else rects.add(reRect);
    	}
    }
	public void initValues(){
    	if(rects.size()==0) return;
    	rectsInfo=new ArrayList<MenpaiHelper.OneRectInfo>();
    	
		for (Bitmap oneRectBitmap : rectBitmaps) {
			ArrayList<ArrayList<ReadInfo>> reList = new ArrayList<ArrayList<ReadInfo>>();
			ArrayList<Bitmap> onecutbitmap = cut(oneRectBitmap);
			Log.d("fax", "cutsize:"+onecutbitmap.size());
			if(onecutbitmap.size()==0) return;
	    	int removeStartCount=0;
	    	while(!isBitmapRight(onecutbitmap.get(0))){
	    		removeStartCount++;
	    		onecutbitmap.remove(0);
				if(onecutbitmap.size()==0) return;
	    	}
	    	int removeEndCount=0;
	    	while(!isBitmapRight(onecutbitmap.get(onecutbitmap.size()-1))){
	    		removeEndCount++;
	    		onecutbitmap.remove(onecutbitmap.size()-1);
				if(onecutbitmap.size()==0) return;
	    	}
	    	Log.d("fax", "removeStartCount:"+removeStartCount+",removeEndCount"+removeEndCount);
	    	
			int cutbitmapsize = onecutbitmap.size();
			for (int i = 0; i < cutbitmapsize; i++) {
				if (checkZifu) {
					int type = bitmapType_all;
					ArrayList<ReadInfo> infolist = readBitmap(onecutbitmap.get(i), type);
					if (infolist != null)
						reList.add(infolist);
				}
			}
			rectsInfo.add(new OneRectInfo(reList));
		}
    }
    //获得一列的颜色类型信息数组
    public int[] getYLineColorType(Bitmap bitmap,int y){
    	int wantwidth=bitmap.getWidth();
		int[] linetype=new int[wantwidth];
		for(int x=0;x<wantwidth;x++){
			int color=bitmap.getPixel(x, y);
			if(ColorHelp.isBlue(color)) linetype[x]=COLOR_TYPE_BLUE;
			else if(ColorHelp.isWhite(color)) linetype[x]=COLOR_TYPE_WHITE;
			else if(ColorHelp.isBlack(color)) linetype[x]=COLOR_TYPE_BLACK;
			else linetype[x]=COLOR_TYPE_OTHER;
		}
		return linetype;
    }
    //去除孤单行
    public void removeSingleLine(ArrayList<Integer> yLineKeys,HashMap<Integer,ArrayList<int[]>> rightYLine){
    	@SuppressWarnings("unchecked")
		ArrayList<Integer> clonelist=(ArrayList<Integer>) yLineKeys.clone();
    	for(int i:clonelist){
    		int maycenter=0;
    		if(clonelist.contains(i-3)) maycenter++;
    		if(clonelist.contains(i-2)) maycenter++;
    		if(maycenter>=2) break;
    		if(clonelist.contains(i-1)) maycenter++;
    		if(maycenter>=2) break;
    		if(clonelist.contains(i+1)) maycenter++;
    		if(maycenter>=2) break;
    		if(clonelist.contains(i+2)) maycenter++;
    		if(maycenter>=2) break;
    		if(clonelist.contains(i+3)) maycenter++;
    		if(maycenter<=1){
    			yLineKeys.remove(Integer.valueOf(i));
    			rightYLine.remove(i);
    		}
    	}
    }
    //获得连续正确的Y范围集合，可能会有多个范围（门牌一般>2），int[0]是start，int[1]是length;
    public ArrayList<int[]> getChepaiYRange(ArrayList<Integer> yLineKeys,HashMap<Integer,ArrayList<int[]>> rightYLine){
    	ArrayList<int[]> relist=new ArrayList<int[]>();
    	int starty=0;
    	int lasty=0;
    	int length=1;
    	
    	float wantpercent=0;
    	int wantpercentLimit=10;
    	for(int i:yLineKeys){
    		if(starty==0){
    			starty=i;
    			lasty=i;
    	    	length=1;
    			int vidsum=0;
    			int linelengthsum=0;
    			for(int[] ints:rightYLine.get(i)){
    				vidsum+=ints[2];
    				linelengthsum+=ints[1];
    			}
    			wantpercent+=linelengthsum/rightYLine.get(i).size()/20f;
    			wantpercent-=vidsum/rightYLine.get(i).size()/10f;
    		}else if(i-lasty<=3){
    			int sub=i-lasty;
    			switch(sub){
    			case 1:wantpercent+=4;break;
    			case 2:wantpercent+=2.5f;break;
    			case 3:wantpercent+=1;break;
//    			case 4:wantpercent+=1;break;
    			}
    			lasty+=sub;
    			length+=sub;
    			int vidsum=0;
    			int linelengthsum=0;
    			for(int[] ints:rightYLine.get(i)){
    				vidsum+=ints[2];
    				linelengthsum+=ints[1];
    			}
    			wantpercent+=linelengthsum/rightYLine.get(i).size()/20f;
    			wantpercent-=vidsum/rightYLine.get(i).size()/10f;
    		}else{
//    	    	Log.d("fax", "wantpercent："+wantpercent);
    			if(wantpercent>wantpercentLimit){
    				relist.add(new int[]{starty,length});
    			}
    	    	starty=i;
    			lasty=i;
    	    	length=1;
    	    	wantpercent=0;

    			int vidsum=0;
    			int linelengthsum=0;
    			for(int[] ints:rightYLine.get(i)){
    				vidsum+=ints[2];
    				linelengthsum+=ints[1];
    			}
    			wantpercent+=linelengthsum/rightYLine.get(i).size()/20f;
    			wantpercent-=vidsum/rightYLine.get(i).size()/10f;
    		}
    	}
//    	Log.d("fax", "wantpercent："+wantpercent);
    	if(wantpercent>wantpercentLimit){
			relist.add(new int[]{starty,length});
		}
    	return relist;
    }
    //获得最有可能是车牌的一个X范围，int[0]是start，int[1]是length;
    public int[] getChepaiXRange(HashMap<Integer,ArrayList<int[]>> rightYLine,int rect_top,int rect_height){
    	ArrayList<Integer> leftxList=new ArrayList<Integer>();
    	ArrayList<Integer> rightxList=new ArrayList<Integer>();
    	for(int i=0;i<rect_height;i++){
    		ArrayList<int[]> lineInfo=rightYLine.get(rect_top+i);
    		if(lineInfo!=null){
    			leftxList.add(lineInfo.get(0)[0]);
    			int[] rightxpartInfo=lineInfo.get(lineInfo.size()-1);
    			rightxList.add(rightxpartInfo[0]+rightxpartInfo[1]-1);
    		}else{
    			Log.d("fax", "lineInfo is null at line:"+i);
    		}
    	}
    	
    	Collections.sort(leftxList);
    	Collections.sort(rightxList);
    	int size=leftxList.size();
//    	Log.d("fax", "leftxList.size:"+size);
    	int removenum=size/4;
//    	Log.d("fax", "removenum"+removenum);
    	for(int i=0;i<removenum;i++){
    		leftxList.remove(0);
    		rightxList.remove(0);
    		leftxList.remove(leftxList.size()-1);
    		rightxList.remove(rightxList.size()-1);
    	}
//    	rightxList=(ArrayList<Integer>) rightxList.subList(removenum, size-removenum);
    	
    	int sum=0;
    	for(int i:leftxList){
    		sum+=i;
    	}
    	int rectLeft=sum/leftxList.size();
    	sum=0;
    	for(int i:rightxList){
    		sum+=i;
    	}
    	int rectRight=sum/rightxList.size();
    	return new int[]{rectLeft,rectRight+1-rectLeft};
    }
    //从一列颜色类型信息数组中获得是蓝白相间的数组信息(可能一列会有多个)，int[0]是start点x值，int[1]是该符合条件数组的length，int[2]代表蓝白颜色比例（0-40），40为比例最失衡，0为比例1：1
    @SuppressWarnings("unused")
	public ArrayList<int[]> getBlueWhiteLineInfo(int[] linetype){
    	if(linetype==null){
    		Log.e("fax", "linetype is null!");
    		return null;
    	}
    	ArrayList<int[]> templist=new ArrayList<int[]>();
    	int length=linetype.length;
    	
//    	int[] linetypecopy=linetype.clone();
    	for(int i=10;i<length-10;i++){
    		if (linetype[i]==COLOR_TYPE_BLACK) {
    			int maychange=0;
    			if(linetype[i-2]>0) maychange++;
    			if(linetype[i-1]>0) maychange++;
    			if(linetype[i+1]>0) maychange++;
    			if(linetype[i+2]>0) maychange++;
    			if(maychange>=2){
    				linetype[i]=COLOR_TYPE_BLUE;
    			}
//				if (linetype[i - 1] * linetype[i + 1] == 2 || linetype[i - 2] * linetype[i + 1] == 2 || linetype[i - 1] * linetype[i + 2] == 2)
//					linetype[i]=COLOR_TYPE_BLUE;
			}
    		if(linetype[i]==COLOR_TYPE_OTHER){
    			int maychange=0;
    			if(linetype[i-2]>0) maychange++;
    			if(linetype[i-1]>0) maychange++;
    			if(linetype[i+1]>0) maychange++;
    			if(linetype[i+2]>0) maychange++;
    			if(maychange>=3){
    				linetype[i]=COLOR_TYPE_WHITE;
    			}
    		}
    		if(linetype[i]==COLOR_TYPE_WHITE||linetype[i]==COLOR_TYPE_BLUE){
    			if(linetype[i-2]<=0&&linetype[i-1]<=0&&linetype[i+1]<=0&&linetype[i+2]<=0){
    				linetype[i]=COLOR_TYPE_OTHER;
    			}
    		}
    	}
//    	linetypecopy=null;

    	
    	int startx=0;
    	int endx=0;
    	int maxlength=0;
    	float maxdiv=0;
    	int maxlengthstart=0;
    	int lastsum=0;
    	for(int i=10;i<length-10;i++){
    		if(linetype[i]>0){
    			if(startx==0) startx=i;
    			lastsum+=linetype[i];
    		}else{
    			if(startx!=0){
    				endx=i;
    				int templength=endx-startx;
    				if(templength<3*2) continue;
    				if(startx+templength/2<15*2||startx+templength/2>85*2) continue;
//					Log.d("fax", "find lengh:"+templength);
					float div=lastsum/(float)templength;
//					Log.d("fax", "div value:"+div);
					if (div>=1.2f&&div<=1.8f) {
						templist.add(new int[]{startx,templength,(int) (Math.abs(div-1.5f)*100)});
						if(templength>maxlength){
    						maxlength = templength;
							maxlengthstart=startx;
							maxdiv=div;
						}
    				}
    				startx=0;
    				lastsum=0;
    			}
    		}
    	}
    	if(maxlength>15){
//    		templist.add(new int[]{maxlengthstart,maxlength,(int) ((maxdiv-1)*100)});
    		return templist;
    	}
    	return null;
    }

    
    
    public ArrayList<Bitmap> cut(Bitmap chepaiBitmap){
    	int chepaiheight=chepaiBitmap.getHeight();
    	ArrayList<int[]> partsInfo=cutLineInfo(getCutedByXLineArray(chepaiBitmap));
    	int size=partsInfo.size();
    	ArrayList<Bitmap> bitmaps=new ArrayList<Bitmap>();
    	for(int i=0;i<size;i++){
    		Bitmap tempbitmap=Bitmap.createBitmap(chepaiBitmap, partsInfo.get(i)[0], 0, partsInfo.get(i)[1], chepaiheight);
//    		Bitmap temp=Bitmap.createBitmap(partsInfo.get(i)[1],chepaiheight,Bitmap.Config.RGB_565);
//    		Canvas cv=new Canvas(temp);
//    		cv.drawBitmap(chepaiBitmap, -partsInfo.get(i)[0],0,null);
    		bitmaps.add(getlimitYBitmap(tempbitmap));
    	}
    	return bitmaps;
    }
    //切割掉上下多余的地方
    public Rect limitY(Bitmap chepaiBitmap){
    	int chepaiwidth=chepaiBitmap.getWidth();
    	int chepaiheight=chepaiBitmap.getHeight();
    	boolean[] ybools=new boolean[chepaiheight];
    	for(int y=0;y<chepaiheight;y++){
    		boolean[] bools=new boolean[chepaiwidth];
    		for(int x=0;x<chepaiwidth;x++){
    			bools[x]=ColorHelp.isWhite(chepaiBitmap.getPixel(x, y));
    		}
    		ybools[y]=isBoolsTrue(bools, .1f);
    	}
    	int[] yinfo=getContinueBoolsInfo(ybools, 1);
    	if(yinfo[1]>chepaiheight*2/3){
//    		if((yinfo[0]-1<=1||whitePointScaleInArea(chepaiBitmap, new Rect(0, 0, chepaiwidth-1, yinfo[0]-1))<0.2f)
//    				&&(chepaiheight-yinfo[0]-yinfo[1]<=1||-whitePointScaleInArea(chepaiBitmap, new Rect(0, yinfo[0]+yinfo[1]-1, chepaiwidth-1, chepaiheight-1))<0.2f)){
    			return new Rect(0,yinfo[0], chepaiwidth-1, yinfo[0]+yinfo[1]-1);
//    		}
    	}
    	Log.d("fax", "maxlength find check fail,use normal way to limit Y");
    	int start=0;int end=chepaiheight-1;
    	for(int i=0;i<chepaiheight/2;i++){
    		if(ybools[i]&&ybools[i+1]){
    			start=i;
    			break;
    		}
    	}
    	for(int i=chepaiheight-1;i>=chepaiheight/2;i--){
    		if(ybools[i]&&ybools[i-1]){
    			end=i;
    			break;
    		}
    	}
    	if(end-start<chepaiheight*2/3){
    		Log.e("fax", "limitY_cut to much,cancle cut");
        	return null;
    	};
    	return new Rect(0,start, chepaiwidth-1, end);
    }
    //切割掉上下多余的地方
    public Bitmap getlimitYBitmap(Bitmap chepaiBitmap){
    	Rect rect=limitY(chepaiBitmap);
    	if(rect==null) return chepaiBitmap;
//    	Bitmap bitmap=Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.RGB_565);
//    	Canvas canvas=new Canvas(bitmap);
//    	canvas.drawBitmap(chepaiBitmap, -rect.left, -rect.top,null);
    	Bitmap bitmap=Bitmap.createBitmap(chepaiBitmap,rect.left,rect.top,rect.width(),rect.height());
    	chepaiBitmap.recycle();
    	return bitmap;
    }
    
    //输入定位后的车牌图形，获得旋转修正的车牌图像
    //暂不可用
//    @Deprecated
    public Bitmap fixRotate(Bitmap chepaiBitmap){
    	int chepaiwidth=chepaiBitmap.getWidth();
    	int chepaiheight=chepaiBitmap.getHeight();
    	int center=(int)(chepaiBitmap.getWidth()/2);
    	boolean[] leftBools=new boolean[chepaiheight];
    	boolean[] rightBools=new boolean[chepaiheight];
    	for(int y=0;y<chepaiheight;y++){
    		boolean[] bools=new boolean[center];
    		for(int x=0;x<center;x++){
    			bools[x]=ColorHelp.isWhite(chepaiBitmap.getPixel(x, y));
    		}
    		leftBools[y]=isBoolsTrue(bools, .1f);
    	}
    	for(int y=0;y<chepaiheight;y++){
    		boolean[] bools=new boolean[chepaiwidth-center];
    		for(int x=center;x<chepaiwidth;x++){
    			bools[x-center]=ColorHelp.isWhite(chepaiBitmap.getPixel(x, y));
    		}
    		rightBools[y]=isBoolsTrue(bools, .1f);
    	}
    	int[] leftInfo=getContinueBoolsInfo(leftBools, chepaiheight/20);
    	int[] rightInfo=getContinueBoolsInfo(rightBools, chepaiheight/20);
    	float y1=leftInfo[0]+leftInfo[1]/2f;
    	float y2=rightInfo[0]+rightInfo[1]/2f;
    	float angle=(float) Math.toDegrees(Math.atan2(y2-y1, center/2f));
    	Log.d("fax", "Bitmap angle:"+angle);
    	if(angle<5&&angle>-5) return chepaiBitmap;
    	Bitmap bitmap_fix=Bitmap.createBitmap(chepaiwidth, chepaiheight, Bitmap.Config.RGB_565);
    	Canvas cv=new Canvas(bitmap_fix);
    	cv.rotate(-angle,chepaiwidth/2,chepaiheight/2);
    	cv.drawBitmap(chepaiBitmap, 0, 0, null);
//    	bitmap.recycle();
    	return bitmap_fix;
    }
    
    //输入布尔数组(scale为阀值，true的比例大于这个值则整个数组为true
    public boolean isBoolsTrue(boolean[] in,float scale){
    	int count=0;
    	for(boolean b: in){
    		if(b){
    			count++;
    		}
    	}
    	return ((float)count/in.length)>scale;
    }
    
    //输入布尔数组(realFalse为该位置之后连续该数个false后才判定为false)，输出最长连续true的起始位置和长度，int[0]:start位置，int[1]：length
    public int[] getContinueBoolsInfo(boolean[] in,int realFalse){
    	int start=-1;
    	int maxlength=1;
    	int maxstart=0;
//    	StringBuilder sb=new StringBuilder();
//    	sb.append("getContinueBoolsInfo:");
//    	for(int i=0;i<in.length;i++){
//    		sb.append(in[i]).append(" ");
//    	}
//    	Log.d("fax", sb.toString());
    	
    	A: for(int i=0;i<in.length;i++){
    		if(in[i]){
    			if(start==-1){
    				start=i;
    			}
    		}else{
    			for(int j=1;j<=realFalse;j++){
    				if(i+j>=in.length||in[i+j]){
    					continue A;
    				}
				}
				if (i - start > maxlength) {
					maxlength = i - start;
					maxstart = start;
				}
				start = i + 1;
			}
    	}
		// 防止一直到数组最后还是true而不结算统计的情况
		if (in.length - start > maxlength) {
			maxlength = in.length - start;
			maxstart = start;
		}
		maxlength++;//多要一个长度像素
//		Log.d("fax", "maxstart:"+maxstart);
//		Log.d("fax", "maxlength:"+maxlength);
		return new int[]{maxstart,maxlength};
	}
    //输入该列的颜色直方图数组，输出切割信息int[0]起始点，int[1]长度
    public ArrayList<int[]> cutLineInfo(float[] in){
    	ArrayList<int[]> list=new ArrayList<int[]>();
    	int length=in.length;
    	float widthlimit=in.length/50;
    	int count=0;
    	for(int i=0;i<length;i++){
    		if(in[i]>0.06f){
    			count++;
    		}else{
    			if(count==0) continue;
    			if(count>=widthlimit) list.add(new int[]{i-count,count});
    			count=0;
    		}
    	}
		if(count>=widthlimit) list.add(new int[]{length-count,count});
		
     	return list;
    }
    private static final int bitmapType_hanzi=3;
    private static final int bitmapType_zimu=2;
    private static final int bitmapType_shuzi=1;
    private static final int bitmapType_zimu_shuzi=0;
    private static final int bitmapType_all=-1;
    //	String为字符		Point为分割方式（x块，y块），为0则分为像素长度块，（1，0）为按每行分，（0，1）为按每列分
    private static HashMap<String,HashMap<Point,float[]>> source_all=new HashMap<String,HashMap<Point,float[]>>();
    private static HashMap<String,HashMap<Point,float[]>> source_hanzi=new HashMap<String,HashMap<Point,float[]>>();
    private static HashMap<String,HashMap<Point,float[]>> source_zimu=new HashMap<String,HashMap<Point,float[]>>();
    private static HashMap<String,HashMap<Point,float[]>> source_shuzi=new HashMap<String,HashMap<Point,float[]>>();
    private static HashMap<String,HashMap<Point,float[]>> source_zimu_shuzi=new HashMap<String,HashMap<Point,float[]>>();
    private static String zifu_hanzi[]=new String[]{"京","津","沪","渝","冀","豫","云","辽","黑","湘","皖","鲁","苏"
    		,"赣","浙","粤","鄂","桂","甘","晋","蒙","陕","吉","闽","贵","青","藏","川","宁","新","琼"};
    private static String getHanziByAssetsFileName(String assetsFileName){
    	if(assetsFileName.equals("z_chuan")) return "川";
    	if(assetsFileName.equals("z_e")) return "鄂";
    	if(assetsFileName.equals("z_gan")) return "甘";
    	if(assetsFileName.equals("z_gan4")) return "赣";
    	if(assetsFileName.equals("z_gui")) return "桂";
    	if(assetsFileName.equals("z_gui2")) return "贵";
    	if(assetsFileName.equals("z_hei")) return "黑";
    	if(assetsFileName.equals("z_hu")) return "沪";
    	if(assetsFileName.equals("z_ji2")) return "吉";
    	if(assetsFileName.equals("z_ji4")) return "冀";
    	if(assetsFileName.equals("z_jin")) return "津";
    	if(assetsFileName.equals("z_jin4")) return "晋";
    	if(assetsFileName.equals("z_jing")) return "京";
    	if(assetsFileName.equals("z_jing_fix")) return "京 ";
    	if(assetsFileName.equals("z_liao")) return "辽";
    	if(assetsFileName.equals("z_lu")) return "鲁";
    	if(assetsFileName.equals("z_meng")) return "蒙";
    	if(assetsFileName.equals("z_min")) return "闽";
    	if(assetsFileName.equals("z_ning")) return "宁";
    	if(assetsFileName.equals("z_qing")) return "青";
    	if(assetsFileName.equals("z_qiong")) return "琼";
    	if(assetsFileName.equals("z_shan")) return "陕";
    	if(assetsFileName.equals("z_su")) return "苏";
    	if(assetsFileName.equals("z_wan")) return "皖";
    	if(assetsFileName.equals("z_xiang")) return "湘";
    	if(assetsFileName.equals("z_xin")) return "新";
    	if(assetsFileName.equals("z_yu2")) return "渝";
    	if(assetsFileName.equals("z_yu4")) return "豫";
    	if(assetsFileName.equals("z_yue")) return "粤";
    	if(assetsFileName.equals("z_yun")) return "云";
    	if(assetsFileName.equals("z_yun_fix")) return "云 ";
    	if(assetsFileName.equals("z_zang")) return "藏";
    	if(assetsFileName.equals("z_zhe")) return "浙";
    	return "字";
    }
    private static String zifu_shuzi[]=new String[]{"0","1","2","3","4","5","6","7","8","9"};
    private static String zifu_zimu[]=new String[]{"A","B","C","D","E","F","G","H","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
    static boolean initing=false;
    static public void initSource(AssetManager assetmanager,Context context){
    	if(source_all.size()!=0||initing) return;
    	initing=true;
    	String path="menpai_img";
    	String[] list=null;
    	try {
    		list=assetmanager.list(path);
		} catch (Exception e) {
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
		}
		for (String s : list) {
			String zimu = s.substring(0, s.length()-4);
			Log.d("fax", "initing:" + zimu);
			Bitmap bitmap = null;
			try {
				bitmap=BitmapFactory.decodeStream(assetmanager.open(path + "/" + s));
			} catch (Exception e) {
				Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
			}
			initSource(zimu, bitmap);
		}
    	initing=false;
    }
    //初始化一个字符特征数据，zifi：要初始化的字符（文件名），bitmap：绑定的要初始化的图片
    private static void initSource(String zifu,Bitmap bitmap){
    	int type=getBitmapTypeByString(zifu);
    	HashMap<Point, float[]> source=initSourceWithBitmap(bitmap);
    	switch(type){
    	case bitmapType_hanzi:
    		source_hanzi.put(zifu,source );
    		source_all.put(zifu,source);
    		break;
    	case bitmapType_shuzi:
    		source_shuzi.put(zifu,source);
    		source_zimu_shuzi.put(zifu,source);
    		source_all.put(zifu,source);
    		break;
    	case bitmapType_zimu:
    		source_zimu.put(zifu,source);
    		if(!zifu.equalsIgnoreCase("O")) source_zimu_shuzi.put(zifu,source);
    		source_all.put(zifu,source);
    		break;
    	}
    }
    private static int getBitmapTypeByString(String zifu){
    	int charvalue=Integer.valueOf(zifu.split("_")[0]);
    	if(charvalue>=48&&charvalue<=57) return bitmapType_shuzi;
    	if(charvalue>=65&&charvalue<=122) return bitmapType_zimu;
    	return bitmapType_hanzi;
    }
    //获得图片的特征集合
    private static HashMap<Point,float[]> initSourceWithBitmap(Bitmap bitmap){
    	BitmapInfo bitmapinfo=new BitmapInfo(bitmap);
    	HashMap<Point,float[]> zifuSource=new HashMap<Point,float[]>();
    	zifuSource.put(new Point(4, 8), getCutedArray(bitmapinfo, 4, 8));
    	zifuSource.put(new Point(3, 7), getCutedArray(bitmapinfo, 3, 7));
    	return zifuSource;
    }
    public boolean isBitmapRight(Bitmap zifubitmap){
    	int width=zifubitmap.getWidth();
    	int height=zifubitmap.getHeight();
    	if((float)height/width>3){
    		Log.d("fax", "height/width>4");
    		return false;
    	}
    	if(width>height*2){
    		Log.d("fax", "width>height*2");
    		return false;
    	}
    	return true;
    }
    //将36335_?转换为36335对应的字符：路
    public String getHanziByASCIIFileName(String ASCIIFileName){
    	if(ASCIIFileName.split("_")[1].startsWith("-")){
    		return "null";
    	}
    	int zifuvalue=Integer.valueOf(ASCIIFileName.split("_")[0]);
    	char zifuchar=(char) zifuvalue;
    	return zifuchar+"";
    }
    //将一个无多余内容的单个字符图形输入,bitmapType为图形类型（汉字3，字母2，数字1，字母或数字0）,读出字符
    public ArrayList<ReadInfo> readBitmap(Bitmap bitmap,int bitmapType){
    	ArrayList<ReadInfo> readInfos=new ArrayList<MenpaiHelper.ReadInfo>();
    	
    	int width=bitmap.getWidth();
    	int height=bitmap.getHeight();
    	if(height/width>5&&whitePointScaleInArea(bitmap, null)>0.7f){
//	    	Log.d("fax", "adaptValue:0.75,adapt:1");
	    	readInfos.add(new ReadInfo("1", 0.75f));
    		return readInfos;
    	}
    	if((float)height/width>3f) return null;
    	if((float)width/height>2f) return null;
    	
		HashMap<String, HashMap<Point, float[]>> source = getSourceByBitmapType(bitmapType);
		for (String zifu : source.keySet()) {
			float value = getAdaptValue(bitmap, source.get(zifu));
			value+=adjustValue(bitmap, zifu);
			
			zifu=getHanziByASCIIFileName(zifu);
			
			if (value > 0.5f){
				readInfos.add(new ReadInfo(zifu, value));
			}
		}
		if(readInfos.size()==0) return null;
		Collections.sort(readInfos,Collections.reverseOrder());
		if(readInfos.get(0).zifu.endsWith("null")) return null;
		while(readInfos.size()>5){
			readInfos.remove(readInfos.size()-1);
		}
    	return readInfos;
    }
    //根据字符关键特征调整匹配值
    public float adjustValue(Bitmap bitmap,String zifu){
    	float adjustValue=0;
		if(getBitmapTypeByString(zifu)==bitmapType_zimu) adjustValue-=0.05;
		if(zifu.equalsIgnoreCase("D")){
			//防止D被误识别为0
			if(whitePointScaleInArea(bitmap, new Rect(1, 0, bitmap.getWidth()/8, bitmap.getHeight()-1))>.9f){
				adjustValue+=0.05;
			}
		}
		return adjustValue;
    }
    
    public HashMap<String,HashMap<Point,float[]>> getSourceByBitmapType(int bitmapType){
    	switch(bitmapType){
    	case bitmapType_hanzi:return source_hanzi;
    	case bitmapType_shuzi:return source_hanzi;
    	case bitmapType_zimu:return source_zimu;
    	case bitmapType_zimu_shuzi:return source_zimu_shuzi;
    	case bitmapType_all:return source_all;
    	}
    	return null;
    }
    //将图形与一个特征集合比较，返回匹配值（0-1）
    public float getAdaptValue(Bitmap bitmap,HashMap<Point, float[]> source){
		float sum=0;
		BitmapInfo bitmapinfo=new BitmapInfo(bitmap);
		sum+=compare(getCutedArray(bitmapinfo, 4, 8), source.get(new Point(4, 8)));
		sum+=compare(getCutedArray(bitmapinfo, 3, 7), source.get(new Point(3, 7)));
		float value=sum/2;
		return value;
    }
    //获得图像以每列分割的白色像素比例数组信息
    public float[] getCutedByXLineArray(Bitmap bitmap){
    	int width=bitmap.getWidth();
		float out[] =new float[width];
		for (int x = 0; x < width; x++) {
			out[x]=whitePointScaleInXLine(bitmap, x);
		}
		return out;
    }
    //获得图像以每列分割的白色像素比例数组信息
    private static float[] getCutedByXLineArray(BitmapInfo bitmap){
    	int width=bitmap.getWidth();
		float out[] =new float[width];
		for (int x = 0; x < width; x++) {
			out[x]=whitePointScaleInXLine(bitmap, x);
		}
		return out;
    }
    //获得图像以每行分割的白色像素比例数组信息
    public float[] getCutedByYLineArray(Bitmap bitmap){
    	int height=bitmap.getHeight();
		float out[] =new float[height];
		for (int y = 0; y < height; y++) {
			out[y]=whitePointScaleInYLine(bitmap, y);
		}
		return out;
    }
    //获得图像以每行分割的白色像素比例数组信息
    private static float[] getCutedByYLineArray(BitmapInfo bitmap){
    	int height=bitmap.getHeight();
		float out[] =new float[height];
		for (int y = 0; y < height; y++) {
			out[y]=whitePointScaleInYLine(bitmap, y);
		}
		return out;
    }
    //bitmap：输入图像，xcount：要分割的X的块数，ycount：要分割的Y的块数（分割总块数是xcount*ycount），输出每一块白色像素比例的数组
    public float[] getCutedArray(Bitmap bitmap,int xcount,int ycount){
    	if(xcount==0) return getCutedByXLineArray(bitmap);
    	if(ycount==0) return getCutedByYLineArray(bitmap);
    	int width=bitmap.getWidth();
    	int heigth=bitmap.getHeight();
    	float stepW=((float)width)/xcount;
		float stepH = ((float) heigth) / ycount;
		float out[] =new float[xcount*ycount];
//		Log.d("fax", "getCutedArray  width:"+width+",heigth:"+heigth+",xcount:"+xcount+",ycount:"+ycount);
		for (int y = 0; y < ycount; y++) {
			for (int x = 0; x < xcount; x++) {
				Rect rect=new Rect((int)(x*stepW), (int)(y*stepH), (int)((x+1)*stepW)-1, (int)((y+1)*stepH)-1);
//				Log.d("fax", "rect:"+rect.left+","+rect.top+","+rect.right+","+rect.bottom);
				out[y*xcount+x]=whitePointScaleInArea(bitmap, rect);
			}
    	}
		return out;
    }
    //bitmap：输入图像，xcount：要分割的X的块数，ycount：要分割的Y的块数（分割总块数是xcount*ycount），输出每一块白色像素比例的数组
    private static float[] getCutedArray(BitmapInfo bitmap,int xcount,int ycount){
    	if(xcount==0) return getCutedByXLineArray(bitmap);
    	if(ycount==0) return getCutedByYLineArray(bitmap);
    	int width=bitmap.getWidth();
    	int heigth=bitmap.getHeight();
    	float stepW=((float)width)/xcount;
		float stepH = ((float) heigth) / ycount;
		float out[] =new float[xcount*ycount];
//		Log.d("fax", "getCutedArray  width:"+width+",heigth:"+heigth+",xcount:"+xcount+",ycount:"+ycount);
		for (int y = 0; y < ycount; y++) {
			for (int x = 0; x < xcount; x++) {
				Rect rect=new Rect((int)(x*stepW), (int)(y*stepH), (int)((x+1)*stepW)-1, (int)((y+1)*stepH)-1);
//				Log.d("fax", "rect:"+rect.left+","+rect.top+","+rect.right+","+rect.bottom);
				out[y*xcount+x]=whitePointScaleInArea(bitmap, rect);
			}
    	}
		return out;
    }
    //比较图形两列数组的相似度(匹配上限是1)
    public float compare(float[] find,float[] source){
    	if(find.length!=source.length){
    		Log.e("fax", "compare:find.length!=source.length!");
    		return 0;
    	}
    	int length=find.length;
    	float value=0;
    	for(int i=0;i<length;i++){
    		if(find[i]==source[i]) {
    			value+=1;
    			continue;
    		}
    		float add=(1-Math.abs(find[i]-source[i]));
    		value+=add;
    	}
    	return value/length;
    }
    //比较图形两列数组的相似度(匹配上限是1)
    public float compareLine(float[] find,float[] source){
    	if(find.length==source.length) return compare(find, source);
    	else if(find.length<source.length){
    		return compare(find, changeIntsLength(source, find.length));
    	}
    	else{
    		return compare(changeIntsLength(find, source.length), source);
    	}
    }
    //改变数组的长度变为length（无平滑效果）
    public float[] changeIntsLength(float[] in,int length){
    	float[] out=new float[length];
    	float step= ((float)in.length)/length;
    	for(int i=0;i<length;i++){
    		out[i]=in[(int) (step*i+0.5f)];
    	}
    	return out;
    }
    //白色像素在x列内所占比例
    public float whitePointScaleInXLine(Bitmap bitmap,int x){
    	int height=bitmap.getHeight();
    	int count=0;
    	for(int y=0;y<height;y++){
    		if(ColorHelp.isWhite(bitmap.getPixel(x, y))) count++;
    	}
    	return (float)count/height;
    }
    //白色像素在x列内所占比例
    private static float whitePointScaleInXLine(BitmapInfo bitmap,int x){
    	int height=bitmap.getHeight();
    	int count=0;
    	for(int y=0;y<height;y++){
    		if((bitmap.getPixel(x, y))) count++;
    	}
    	return (float)count/height;
    }
    //白色像素在y列内所占比例
    public float whitePointScaleInYLine(Bitmap bitmap,int y){
    	int width=bitmap.getWidth();
    	int count=0;
    	for(int x=0;x<width;x++){
    		if(ColorHelp.isWhite(bitmap.getPixel(x, y))) count++;
    	}
    	return (float)count/width;
    }
    //白色像素在y列内所占比例
    private static float whitePointScaleInYLine(BitmapInfo bitmap,int y){
    	int width=bitmap.getWidth();
    	int count=0;
    	for(int x=0;x<width;x++){
    		if((bitmap.getPixel(x, y))) count++;
    	}
    	return (float)count/width;
    }
    //白色像素在区域内所占比例
    public float whitePointScaleInArea(Bitmap bitmap,Rect rect){
    	if(rect==null) rect=new Rect(0, 0, bitmap.getWidth()-1, bitmap.getHeight()-1);
    	int count=0;
    	for(int x=rect.left;x<=rect.right;x++){
    		for(int y=rect.top;y<=rect.bottom;y++){
    			if(ColorHelp.isWhite(bitmap.getPixel(x, y))) count++;
    		}
    	}
    	return count/(float)(rect.height()*rect.width());
    }
    //白色像素在区域内所占比例
    private static float whitePointScaleInArea(BitmapInfo bitmap,Rect rect){
    	if(rect==null) rect=new Rect(0, 0, bitmap.getWidth()-1, bitmap.getHeight()-1);
    	int count=0;
    	for(int x=rect.left;x<=rect.right;x++){
    		for(int y=rect.top;y<=rect.bottom;y++){
    			if((bitmap.getPixel(x, y))) count++;
    		}
    	}
    	return count/(float)(rect.height()*rect.width());
    }
    //用于快速处理比较计算信息的类
    private static class BitmapInfo{
    	int width;
		int height;
    	public int getWidth() {
			return width;
		}
		public int getHeight() {
			return height;
		}
    	boolean[][] info;
    	public BitmapInfo(Bitmap bitmap){
    		width=bitmap.getWidth();
    		height=bitmap.getHeight();
    		info=new boolean[width][height];
    		for(int x=0;x<width;x++){
    			for(int y=0;y<height;y++){
    				info[x][y]=ColorHelp.isWhite(bitmap.getPixel(x, y));
    			}
    		}
    	}
    	public boolean getPixel(int x,int y){
    		return info[x][y];
    	}
    }
    //用于方便信息传输比较的类
    class ReadInfo implements Comparable<ReadInfo>{
    	float value;
    	String zifu;
    	public ReadInfo(String zifu,float value){
    		this.zifu=zifu;
    		this.value=value;
    	}
		@Override
		public int compareTo(ReadInfo another) {
			return (int) ((value-another.value)*10000000);
		}
    	
    }
    //封装一个正确完整矩形图像的信息
    class OneRectInfo{
    	ArrayList<ArrayList<ReadInfo>> list;
    	public OneRectInfo(ArrayList<ArrayList<ReadInfo>> list){
    		this.list=list;
    	}
    }
}
