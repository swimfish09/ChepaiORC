package com.fax.chepai;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;

import com.fax.chepai.ChepaiHelper.ReadInfo;
import com.fax.chepai.MenpaiHelper.OneRectInfo;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

public class MainActivity extends Activity implements OnClickListener{
	private Button but_1;
    private Button but_2;
    private Button but_3;
    private Button but_4;
    private FrameLayout homelayout;
    public Handler handler;
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE); // 设置不显示标题
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.home_screen);
        handler=new Handler();
        
        but_1 = (Button) findViewById(R.id.but_1);
        but_1.setOnClickListener(this);
        but_2 = (Button) findViewById(R.id.but_2);
        but_2.setOnClickListener(this);
        but_3 = (Button) findViewById(R.id.but_3);
        but_3.setOnClickListener(this);
        but_4 = (Button) findViewById(R.id.but_4);
        but_4.setOnClickListener(this);
        homelayout=(FrameLayout) findViewById(R.id.homelayout);
        
        onClick(but_1);
    }

    public void onClick(int id){
    	switch (id) {
		case R.id.but_1:
			setSelectedButton(but_1);
			homelayout.removeAllViews();
			LinearLayout ll=(LinearLayout) LinearLayout.inflate(this, R.layout.container, null);
			ListView list1=new ListView(this);
			ArrayAdapter<String> adapter=new ArrayAdapter<String>(this, R.layout.listres,R.id.listtv, new String[]{"城市基础设施运营管理","市政公用事业综合管理制度","设备维修管理规章制度","环境卫生管理规章制度","车辆管理规章制度与常用表格"});
			list1.setAdapter(adapter);
			list1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					switch(position){
					case 0:
						break;
					case 1:
						break;
					case 2:
						break;
					}
				}
			});
			ll.addView(list1,new LinearLayout.LayoutParams(-1, -2));
			homelayout.addView(ll,new LinearLayout.LayoutParams(-1, -2));
			break;
		case R.id.but_2:
			setSelectedButton(but_2);
			homelayout.removeAllViews();
			TextView textView2=new TextView(this);
			textView2.setText("初始化...");
			textView2.setTextSize(getResources().getDisplayMetrics().widthPixels/16);
			textView2.setGravity(Gravity.CENTER);
			homelayout.addView(textView2,new LinearLayout.LayoutParams(-1, -1));
			handler.post(new OpenCameraRunnable());
			new InitMenpaiSourceThread().start();
			break;
		case R.id.but_3:
			setSelectedButton(but_3);
			homelayout.removeAllViews();
			TextView textView3=new TextView(this);
			textView3.setText("初始化...");
			textView3.setTextSize(getResources().getDisplayMetrics().widthPixels/16);
			textView3.setGravity(Gravity.CENTER);
			homelayout.addView(textView3,new LinearLayout.LayoutParams(-1, -1));
			handler.post(new OpenCameraRunnable());
			new InitChepaiSourceThread().start();
			
			break;
		case R.id.but_4:
			setSelectedButton(but_4);
			homelayout.removeAllViews();
			RelativeLayout page4=(RelativeLayout) RelativeLayout.inflate(this, R.layout.chejianchuli, null);
			homelayout.addView(page4,new RelativeLayout.LayoutParams(-1, -1)); 
			break;
		}
    }
	public void onClick(View v) {
		onClick(v.getId());
	}
	public void setSelectedButton(Button selectedBut){
		if(selectedBut!=but_1){
			but_1.setBackgroundResource(R.drawable.but_normal);
			but_1.setClickable(true);
		}
		if(selectedBut!=but_2){
			but_2.setBackgroundResource(R.drawable.but_normal);
			but_2.setClickable(true);
		}
		if(selectedBut!=but_3){
			but_3.setBackgroundResource(R.drawable.but_normal);
			but_3.setClickable(true);
		}
		if(selectedBut!=but_4){
			but_4.setBackgroundResource(R.drawable.but_normal);
			but_4.setClickable(true);
		}
		selectedBut.setBackgroundResource(R.drawable.but_select);
		selectedBut.setClickable(false);
	}
	private class InitChepaiSourceThread extends Thread{
		@Override
		public void run() {
			ChepaiHelper.initSource(getAssets(), MainActivity.this);
		}
		
	}
	private class InitMenpaiSourceThread extends Thread{
		@Override
		public void run() {
			MenpaiHelper.initSource(getAssets(), MainActivity.this);
		}
		
	}
    private boolean isShowInfo=false;
	//在界面显示信息窗口
	public void showInfo(final ChepaiHelper chepaiHelper){
		isShowInfo=true;
		ArrayList<ArrayList<ReadInfo>> reList=chepaiHelper.reList;
		StringBuilder sb=new StringBuilder();
		for(ArrayList<ReadInfo> zifuInfo:reList){
			for(ReadInfo info:zifuInfo){
				sb.append(info.zifu).append("(").append((int)(info.value*100)).append("),");
			}
			sb.deleteCharAt(sb.length()-1).append("\n");
		}
		final String textInfo=sb.toString();
		handler.post(new Runnable() {
			@Override
			public void run() {
				LinearLayout view=(LinearLayout) LinearLayout.inflate(MainActivity.this, R.layout.chepai_info, null);
				ImageView imageView=(ImageView) view.findViewById(R.id.chepai_info_imageview);
				TextView textview=(TextView) view.findViewById(R.id.chepai_info_textview);
				imageView.setImageBitmap(chepaiHelper.chepaiBitmap);
				textview.setText(textInfo);
				try {
					new AlertDialog.Builder(MainActivity.this).setView(view)
					.setPositiveButton("确定", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							isShowInfo = false;
							initValueable=true;
						}
					})
					.setOnCancelListener(new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							isShowInfo = false;
							initValueable=true;
						}
					})
					.create().show();
				} catch (Exception e) {
				}
			}
		});
		
	}
	//在界面显示信息窗口
	public void showInfo(final MenpaiHelper menpaiHelper){
		isShowInfo=true;
		StringBuilder sb=new StringBuilder();
		for (int i = 0; i < menpaiHelper.rectsInfo.size(); i++) {
			ArrayList<ArrayList<com.fax.chepai.MenpaiHelper.ReadInfo>> reList = menpaiHelper.rectsInfo.get(i).list;
			for (ArrayList<com.fax.chepai.MenpaiHelper.ReadInfo> zifuInfo : reList) {
				for (com.fax.chepai.MenpaiHelper.ReadInfo info : zifuInfo) {
					sb.append(info.zifu).append("(").append((int) (info.value * 100)).append("),");
				}
				sb.deleteCharAt(sb.length() - 1).append("\n");
			}
		}
		final String textInfo=sb.toString();
		handler.post(new Runnable() {
			@Override
			public void run() {
				LinearLayout view=(LinearLayout) LinearLayout.inflate(MainActivity.this, R.layout.menpai_info, null);
				ImageView imageView=(ImageView) view.findViewById(R.id.menpai_info_imageview);
				ImageView imageView2=(ImageView) view.findViewById(R.id.menpai_info_imageview2);
				TextView textview=(TextView) view.findViewById(R.id.menpai_info_textview);
				imageView.setImageBitmap(menpaiHelper.rectBitmaps.get(0));
				imageView2.setImageBitmap(menpaiHelper.rectBitmaps.get(1));
				textview.setText(textInfo);
				try {
					new AlertDialog.Builder(MainActivity.this).setView(view)
					.setPositiveButton("确定", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							isShowInfo = false;
							initValueable=true;
						}
					})
					.setOnCancelListener(new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							isShowInfo = false;
							initValueable=true;
						}
					})
					.create().show();
				} catch (Exception e) {
				}
			}
		});
		
	}
	//在界面显示车牌位置矩形
		public void showRectInView(final Rect rect,final float xscale,final float yscale) {
			handler.post(new Runnable() {
				public void run() {
					FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int)(rect.width()*xscale), (int) (rect.height()*yscale));
					params.setMargins((int)(rect.left*xscale), (int)(rect.top*yscale), 0, 0);
					params.gravity=Gravity.TOP;
					rectView.setLayoutParams(params);
				}
			});
		}
		//在界面显示车牌位置矩形
		public void showRect2InView(final Rect rect,final float xscale,final float yscale) {
			handler.post(new Runnable() {
				public void run() {
					FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int)(rect.width()*xscale), (int) (rect.height()*yscale));
					params.setMargins((int)(rect.left*xscale), (int)(rect.top*yscale), 0, 0);
					params.gravity=Gravity.TOP;
					rectView2.setLayoutParams(params);
				}
			});
		}
	public void disShowRectInView(){
		handler.post(new Runnable() {
			public void run() {
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(0,0);
				rectView.setLayoutParams(params);
				FrameLayout.LayoutParams params2 = new FrameLayout.LayoutParams(0,0);
				rectView2.setLayoutParams(params2);
			}
		});
	}
    //
    private boolean isChepaiOK(ChepaiHelper chepaiHelper){
    	ArrayList<ArrayList<ReadInfo>> list=chepaiHelper.reList;
    	if(list.size()==7){
    		for(ArrayList<ReadInfo> zifu:list){
    			if(zifu.size()==0){
    				return false;
    			}
    		}
    		return true;
    	}
    	return false;
    }
    private boolean isMenpaiOK(MenpaiHelper menpaiHelper){
    	ArrayList<OneRectInfo> rectsInfo=menpaiHelper.rectsInfo;
    	Log.d("fax", "rectsInfo.size():"+rectsInfo.size());
    	if(rectsInfo.size()==2){
    		for(OneRectInfo oneInfo:rectsInfo){
    			ArrayList<ArrayList<com.fax.chepai.MenpaiHelper.ReadInfo>> list=oneInfo.list;
    	    	Log.d("fax", "oneInfo.list.size():"+oneInfo.list.size());
    			for(ArrayList<com.fax.chepai.MenpaiHelper.ReadInfo> zifu:list){
        			if(zifu.size()==0){
        				return false;
        			}
        		}
    		}
    		return true;
    	}
    	return false;
    }
    Matrix matrix;
    boolean initValueable=true;//限制:同一时间只有一个initValue进程;
	private void findAndDealInfo(Camera camera,byte[] mData) {
        if(isShowInfo){
        	disShowRectInView();
        	return;
        }
        //YUV2BMP
        Size size = camera.getParameters().getPreviewSize(); //获取预览大小
        final int w = size.width;  //宽度
        final int h = size.height;
        final YuvImage image = new YuvImage(mData, ImageFormat.NV21, w, h, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream(mData.length);
        if(!image.compressToJpeg(new Rect(0, 0, w, h), 100, os)){
            return;
        }
        byte[] tmp = os.toByteArray();
        final Bitmap bmp = BitmapFactory.decodeByteArray(tmp, 0,tmp.length); 
        
        if(matrix==null){
            matrix=new Matrix();
            matrix.postRotate(90);
        }
		Bitmap rotateBitmap=Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(),bmp.getHeight(),  matrix, false);
        bmp.recycle();

        if(!but_3.isClickable()) findChepaiInfoInBitmap(rotateBitmap);
        else if(!but_2.isClickable()) findMenpaiInfoInBitmap(rotateBitmap);
	}
	public void findChepaiInfoInBitmap(Bitmap bmp){
		//显示车牌矩形
        final ChepaiHelper chepaiHelper=new ChepaiHelper(bmp);
        if(chepaiHelper.reRect!=null) showRectInView(chepaiHelper.reRect,(float)homelayout.getWidth()/bmp.getWidth(),(float)homelayout.getHeight()/bmp.getHeight());
        else{
        	disShowRectInView();
        	return;
        }

		if (!initValueable)
			return;
		initValueable = false;
		chepaiHelper.startInitValueThread(new Runnable() {
			@Override
			public void run() {
	            if(isChepaiOK(chepaiHelper)){
	            	showInfo(chepaiHelper);
	            }else initValueable=true;
			}
        });
	}
	public void findMenpaiInfoInBitmap(Bitmap bmp){
		final MenpaiHelper menpaiHelper=new MenpaiHelper(bmp);
        if(menpaiHelper.rects.size()!=0){
        	showRectInView(menpaiHelper.rects.get(0),(float)homelayout.getWidth()/bmp.getWidth(),(float)homelayout.getHeight()/bmp.getHeight());
        	if(menpaiHelper.rects.size()==2) showRect2InView(menpaiHelper.rects.get(1),(float)homelayout.getWidth()/bmp.getWidth(),(float)homelayout.getHeight()/bmp.getHeight());
        }
        else{
        	disShowRectInView();
        	return;
        }

		if (!initValueable)
			return;
		initValueable = false;
		menpaiHelper.startInitValueThread(new Runnable() {
			@Override
			public void run() {
	            if(isMenpaiOK(menpaiHelper)){
	            	Log.d("fax", "startShowInfo");
	            	showInfo(menpaiHelper);
	            }else initValueable=true;
			}
        });
	}
	ImageView rectView;
	ImageView rectView2;
	Camera camera;
	private class OpenCameraRunnable implements Runnable,SurfaceHolder.Callback, PreviewCallback, OnTouchListener{
		@Override
		public void run() {
			SurfaceView cameraView =new SurfaceView(MainActivity.this);
//			if(cameraView.camera==null) return;
			homelayout.removeAllViews();
			homelayout.addView(cameraView,-1,-1);
			rectView=new ImageView(MainActivity.this);
			rectView.setBackgroundResource(R.drawable.rect);
			homelayout.addView(rectView,0,0);
			rectView2=new ImageView(MainActivity.this);
			rectView2.setBackgroundResource(R.drawable.rect);
			homelayout.addView(rectView2,0,0);
			cameraView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			cameraView.getHolder().addCallback(this);
			cameraView.setOnTouchListener(this);
			camera=android.hardware.Camera.open();
			if(camera==null){
				Toast.makeText(MainActivity.this, "摄像头打开失败", Toast.LENGTH_SHORT).show();
				return;
			}
			
			TextView tishi=new TextView(MainActivity.this);
			tishi.setText("点击屏幕进行重新对焦识别");
			tishi.setTextColor(Color.GREEN);
			tishi.setGravity(Gravity.CENTER);
			FrameLayout.LayoutParams tishiparams=new FrameLayout.LayoutParams(-1, -2);
			tishi.setTextSize(getResources().getDisplayMetrics().widthPixels/24);
			homelayout.addView(tishi, tishiparams);
		}
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			if(camera==null) return;
			Camera.Parameters parameters = camera.getParameters();
			camera.setDisplayOrientation(90);
//			camera.setPreviewCallback(this);
			camera.setParameters(parameters);
			try {
				// 设置显示
				camera.setPreviewDisplay(holder);
			} catch (Exception exception) {
				Log.d("fax", "exception:"+exception.getMessage());
				camera.release();
				camera = null;
			}
			// 开始预览
			camera.startPreview();
			camera.autoFocus(new AutoFocusCallback()
			{
				@Override
				public void onAutoFocus(boolean success, Camera camera){
					if(success) camera.setPreviewCallback(OpenCameraRunnable.this);
					else Toast.makeText(MainActivity.this,"点击屏幕进行对焦识别", Toast.LENGTH_LONG);
				}
			});
		}
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			if(camera!=null) return;
			camera=android.hardware.Camera.open();
			if(camera==null){
				Toast.makeText(MainActivity.this, "摄像头打开失败", Toast.LENGTH_SHORT).show();
				return;
			}
		}
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			camera.setPreviewCallback(null);
			camera.release();
			camera=null;
		}
		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
            findAndDealInfo(camera, data);
		}
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if(event.getAction()==MotionEvent.ACTION_DOWN){
				camera.setPreviewCallback(null);
				disShowRectInView();
				// 自动对焦
				camera.autoFocus(new AutoFocusCallback()
				{
					@Override
					public void onAutoFocus(boolean success, Camera camera){
						if(success) camera.setPreviewCallback(OpenCameraRunnable.this);
					}
				});
			}
			return false;
		}
	}
}
