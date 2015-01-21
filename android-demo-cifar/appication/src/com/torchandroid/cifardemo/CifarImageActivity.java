package com.torchandroid.cifardemo;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.os.AsyncTask;

import com.torchandroid.cifardemo.lua.LuaManager;
import com.torchandroid.cifardemo.util.Util;

public class CifarImageActivity extends Activity {

	final private String TAG = "CifarImageActivity";

	private LuaManager mLuaManager;
	private Button mSelectImageButton;
	private Button mStartRecogButton;
	private Button mButtonCamera;
	private ImageView mImageView;
	private ImageView mSmallImageView;
	private final int REQUEST_CODE_IMAGE = 100;
	private final int REQUEST_CODE_CAMERA = 200;
	private Uri mImageUri;
  Bitmap resizedBitmap;
  AsyncRecognizer ar;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_image_selection);

		mSelectImageButton = (Button) findViewById(R.id.select_image_button);
		mSelectImageButton.setOnClickListener(mSelectImageButtonClickListener);

		mStartRecogButton = (Button) findViewById(R.id.start_recog_button);
		mStartRecogButton.setOnClickListener(mStartRecogButtonClickListener);

		mButtonCamera = (Button) findViewById(R.id.button_camera_cifar);
		mButtonCamera.setOnClickListener(mCamerabuttonClickListener);

		mLuaManager = LuaManager.getLuaManager(this);
		mImageView = (ImageView) findViewById(R.id.image);
		mSmallImageView = (ImageView) findViewById(R.id.image_small);

	}

	/**
	 * send intent to pick an image
	 */
	OnClickListener mSelectImageButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(intent, REQUEST_CODE_IMAGE);
		}
	};

	/**
	 * get selected image uri
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == RESULT_OK) {
			if (requestCode == REQUEST_CODE_IMAGE) {
				mImageUri = data.getData();
				mImageView.setImageURI(mImageUri);
				mStartRecogButton.setText(R.string.start_recog);
			} else if (requestCode == REQUEST_CODE_CAMERA) {
				
				Bundle extras = data.getExtras();
				//get bitmap from returned intent
				Bitmap imageBitmap = (Bitmap) extras.get("data"); 
				mImageView.setImageBitmap(imageBitmap);

				Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, 32,
						32, false);
				//start recognition directly with 32x32 resized image
				int result = mLuaManager.getTopRecognitionResult(32, 32,
						Util.getImageRGBA(resizedBitmap));
				if (result != -1)
					mStartRecogButton.setText("Result : "
							+ Util.classes[result - 1]);
				else
					mStartRecogButton.setText("recognition failed");
				mSmallImageView.setImageBitmap(resizedBitmap);
			}
		}
	};

	/**
	 * get file path from content Uri
	 * @param contentUri
	 * @return file path by string
	 */
	private String getRealPathFromURI(Uri contentUri) {

		String filePath;
		String scheme = contentUri.getScheme();

		if (scheme.equals("file")
				&& contentUri.getEncodedAuthority().equals(
						"org.openintents.filemanager")) {
			// Get the path
			filePath = contentUri.getPath();
			return filePath;
		} else if (scheme.equals("content")) {
			String[] projection = { MediaStore.Images.Media.DATA };
			Cursor cursor = getContentResolver().query(contentUri, projection,
					null, null, null);
			int column_index = cursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		}

		return contentUri.getPath();
	}

  class AsyncRecognizer extends AsyncTask<Void, Void, Void> {
    @Override
    protected Void doInBackground(Void... params) {
      int result = mLuaManager.getTopRecognitionResult(32, 32, Util.getImageRGBA(resizedBitmap));
      return null;
    }
  }

	OnClickListener mStartRecogButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mImageUri != null && mImageUri.getPath() != null) {
				File selectedImgFile = new File(getRealPathFromURI(mImageUri));
				Log.e(TAG, "Image path : " + mImageUri.getPath());
				Bitmap bitmap = BitmapFactory.decodeFile(selectedImgFile
						.getPath());
				if (bitmap != null) {
					Log.e(TAG, "bitmap size : " + bitmap.getWidth() + ","
							+ bitmap.getHeight());


					if (bitmap.getWidth() != 32 || bitmap.getHeight() != 32) {
						Log.d(TAG, "bitmap resized to 32x32");
						resizedBitmap = Bitmap.createScaledBitmap(bitmap, 32,
								32, false);
					} else {
						resizedBitmap = bitmap;
					}
                    /*
					int result = mLuaManager.getTopRecognitionResult(32, 32,
							Util.getImageRGBA(resizedBitmap));
					if (result != -1)
						mStartRecogButton.setText("Result : "
								+ Util.classes[result - 1]);
					else
						mStartRecogButton.setText("recognition failed");
					*/
          ar = new AsyncRecognizer();
          ar.execute();

					mSmallImageView.setImageBitmap(resizedBitmap);
				} else {
					Log.e(TAG, "bitmap null");
				}

			}

		}
	};

	OnClickListener mStartResizeAndRecogButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {

		}
	};

	View.OnClickListener mCamerabuttonClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent();
			intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
			startActivityForResult(intent, REQUEST_CODE_CAMERA);
		}
	};

}
