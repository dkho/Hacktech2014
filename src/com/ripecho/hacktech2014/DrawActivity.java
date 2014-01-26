package com.ripecho.hacktech2014;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;

public class DrawActivity extends Activity implements PopupMenu.OnMenuItemClickListener {
	
	private final static int DEFAULT_DIMENSION = 32;
	
	private String filename = "Untitled";
	private int new_width = DEFAULT_DIMENSION, new_height = DEFAULT_DIMENSION;
	private int curColor = Color.BLACK;
	private boolean colorLock = false;
	private Tool curTool = Tool.PENCIL;
	private Bitmap bitmap;
	private ArrayList<CursorOrigin> cursorOrigins = new ArrayList<CursorOrigin>(10);
	private PopupMenu fileMenu;
	private Button[] buttonList;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.draw_activity);
		
		bitmap = Bitmap.createBitmap(new_width, new_height, Bitmap.Config.ARGB_8888);
		for (int i = 0; i < new_width; i++)
			for (int j = 0; j < new_height; j++)
				bitmap.setPixel(i, j, 0x00000000);
		bitmap.setPixel(0, 0, 0xFFFFFFFF);
		bitmap.setPixel(15, 15, 0xFFFFFFFF);
		
		ColorPaletteView cpv = (ColorPaletteView)findViewById(R.id.color_palette);
		cpv.setParent(this);
		
		PixelGridView pgv = (PixelGridView)findViewById(R.id.pixel_grid);
		pgv.setParent(this);
		
		Button fileButton = (Button)findViewById(R.id.file_button);
		//tbv.setParent(this);
		fileMenu = new PopupMenu(this, fileButton);
		fileMenu.inflate(R.menu.draw_activity_menu);
		fileMenu.setOnMenuItemClickListener(this);
		
		buttonList = new Button[4];
		buttonList[0] = (Button)findViewById(R.id.pencil_button);
		buttonList[1] = (Button)findViewById(R.id.eraser_button);
		buttonList[2] = (Button)findViewById(R.id.eyedropper_button);
		buttonList[3] = (Button)findViewById(R.id.bucket_button);
		
		usePencil(null);
	}
	
	public void testFunc(View v){
		fileMenu.show();
	}
	
	protected void onPause() {
		super.onPause();
		getPreferences(MODE_PRIVATE).edit()
			.clear()
			.putInt("curColor",  curColor)
			.putBoolean("colorLock", colorLock)
			.putInt("curTool", curTool.getValue())
			.apply();
		try
		{
			FileOutputStream temp = new FileOutputStream(new File(getFilesDir(), "temp.png"));
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, temp);
		}
		catch (FileNotFoundException e)
		{
			
		}
	}
	
	public void setColor(int a, int r, int g, int b) {	
		curColor = Color.argb(a, r, g, b);
	}
	
	public void setColor(int argb) {
		curColor = argb;
	}

	public int getColor() {
		return curColor;
	}
	
	public void setTool(Tool t) {
		curTool = t;
	}
	
	public Tool getTool() {
		return curTool;
	}
	
	public boolean getIsLocked() {
		return colorLock;
	}
	
	public void openFileMenu(View v) {
		fileMenu.show();
	}
	
	public Bitmap getBitmap() {
		return bitmap;
	}
	
	public void setBitmap(String pathName) {
		bitmap = BitmapFactory.decodeFile(pathName);
	}
	
	public void drawToBitmap(int x, int y, int argb) {
		bitmap.setPixel(x, y, argb);
	}
	
	public void addCursorOrigin(int id, ViewOrigin origin) {
		for (int i = 0; i < cursorOrigins.size(); i++) {
			if (cursorOrigins.get(i).id == id) {
				cursorOrigins.get(i).origin = origin;
				break;
			}
		}
				
		cursorOrigins.add(new CursorOrigin(id, origin));
	}
	
	public void removeCursorOrigin(int id) {
		for (int i = 0; i < cursorOrigins.size(); i++) {
			if (cursorOrigins.get(i).id == id) {
				cursorOrigins.remove(i);
				break;
			}
		}
	}
	
	public ViewOrigin getCursorOrigin(int id) {
		for (int i = 0; i < cursorOrigins.size(); i++)
			if (cursorOrigins.get(i).id == id)
				return cursorOrigins.get(i).origin;
		return ViewOrigin.NULL;
	}
	
	private class CursorOrigin {
		public int id;
		public ViewOrigin origin;
		
		public CursorOrigin(int id, ViewOrigin origin) {
			this.id = id;
			this.origin = origin;
		}
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId())
		{
		case R.id.new_menu:
			filename = "Untitled";
			bitmap = Bitmap.createBitmap(DEFAULT_DIMENSION, DEFAULT_DIMENSION, Bitmap.Config.ARGB_8888);
			return true;
		case R.id.save_menu:
			try
			{
				Log.d("DEBUG", "" + Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()));
				FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStoragePublicDirectory(
			            Environment.DIRECTORY_PICTURES), filename + ".png"));
				if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos))
					Log.d("DEBUG", "true");
				fos.close();
				Log.d("DEBUG",Environment.getExternalStoragePublicDirectory(
			            Environment.DIRECTORY_PICTURES).getAbsolutePath());
			}
			catch (FileNotFoundException e)
			{
				Log.d("DEBUG", "ERROR");
			}
			catch (IOException e)
			{
			}
			return true;
		case R.id.options_menu:
			Intent intent = new Intent(this, OptionsActivity.class);
			startActivity(intent);
			//getFragmentManager().beginTransaction().replace(android.R.id.content, new OptionsActivity()).commit();
			return true;
		}
		return false;
	}
	
	protected void onRestart()
	{
		super.onRestart();
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		// Replace bitmap if different size is specified
		
		if ((Integer.parseInt(sp.getString("width_key", "-1")) != bitmap.getWidth() 
				|| Integer.parseInt(sp.getString("height_key", "-1")) != bitmap.getHeight()) &&
				Integer.parseInt(sp.getString("width_key", "-1")) > 0 && Integer.parseInt(sp.getString("height_key", "-1")) > 0)
		{
			Bitmap new_bitmap = Bitmap.createBitmap(Integer.parseInt(sp.getString("width_key", "-1")), Integer.parseInt(sp.getString("height_key", "-1")), Bitmap.Config.ARGB_8888);
			for (int i = 0; i < bitmap.getWidth(); i++)
				for (int j = 0; j < bitmap.getHeight(); j++)
					if (i < new_bitmap.getWidth() && j < new_bitmap.getHeight())
						new_bitmap.setPixel(i, j, bitmap.getPixel(i, j));
			bitmap = new_bitmap;
			((PixelGridView)findViewById(R.id.pixel_grid)).updateSize();
		}
		filename = sp.getString("filename_key",  "Untitled");
	}
	
	public void usePencil(View v)
	{
		curTool = Tool.PENCIL;
		resetButtons();
		buttonList[0].setEnabled(false);
	}
	
	public void useEraser(View v)
	{
		curTool = Tool.ERASER;
		resetButtons();
		buttonList[1].setEnabled(false);
	}
	
	public void useEyedropper(View v)
	{
		curTool = Tool.EYE_DROPPER;
		resetButtons();
		buttonList[2].setEnabled(false);
	}
	
	public void useBucket(View v)
	{
		curTool = Tool.BUCKET;
		resetButtons();
		buttonList[3].setEnabled(false);
	}
	
	private void resetButtons()
	{
		for (int i = 0; i < buttonList.length; i++)
			buttonList[i].setEnabled(true);
	}
}
