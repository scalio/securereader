package info.guardianproject.securereaderinterface;

import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereader.XMLRPCPublisher;
import info.guardianproject.securereader.XMLRPCPublisher.XMLRPCPublisherCallback;
import info.guardianproject.securereaderinterface.ui.MediaViewCollection;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers.FadeInFadeOutListener;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.views.CreateAccountView;
import info.guardianproject.securereaderinterface.views.PostSignInView;
import info.guardianproject.securereaderinterface.views.StoryMediaContentView;
import info.guardianproject.securereaderinterface.views.CreateAccountView.OnActionListener;
import info.guardianproject.securereaderinterface.views.PostSignInView.OnAgreeListener;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.iocipher.File;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.tinymission.rss.Item;
import com.tinymission.rss.MediaContent;

public class AddPostActivity extends FragmentActivityWithMenu implements OnActionListener, OnFocusChangeListener, OnAgreeListener,
		FadeInFadeOutListener
{
	
	public static final String LOGTAG = "AddPostActivity";
	public static final boolean LOGGING = false;
	
	ProgressDialog loadingDialog;

	private static final int REQ_CODE_PICK_IMAGE = 1;
	private StoryMediaContentView mMediaView;
	private EditText mEditTitle;
	private EditText mEditContent;
	private EditText mEditTags;
	private Item mStory;
	private CreateAccountView mViewCreateAccount;
	private PostSignInView mViewSignIn;
	private View mBtnMediaAdd;
	private View mBtnMediaAddMore;
	private View mBtnMediaReplace;
	private View mBtnMediaView;
	private View mBtnMediaDelete;
	private View mOperationButtons;
	private View mProgressIcon;
	private java.io.File mCurrentPhotoFile;
	private int mReplaceThisIndex;
	private boolean mIsAddingMedia;
	private AlertDialog mMediaChooserDialog;

	private Intent mStartedIntent;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_post);

		// Display home as up
		setDisplayHomeAsUp(true);

		setMenuIdentifier(R.menu.activity_add_post);
		mReplaceThisIndex = -1;
		mIsAddingMedia = false;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		this.setContentView(R.layout.activity_add_post);
	}

	@Override
	public void onSupportContentChanged()
	{
		super.onSupportContentChanged();
		mEditTitle = (EditText) findViewById(R.id.editTitle);
		mEditContent = (EditText) findViewById(R.id.editContent);
		mEditTags = (EditText) findViewById(R.id.editTags);

		mMediaView = (StoryMediaContentView) findViewById(R.id.mediaContentView);

		mMediaView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (!mIsAddingMedia && mOperationButtons.getVisibility() == View.GONE)
				{
					mOperationButtons.setVisibility(View.VISIBLE);
					AnimationHelpers.fadeIn(mOperationButtons, 500, 5000, false);
				}
			}
		});
		mMediaView = (StoryMediaContentView) findViewById(R.id.mediaContentView);
		mMediaView.setShowPlaceholder(true);
		mMediaView.setShowPlaceholderWhileLoading(true);

		mOperationButtons = findViewById(R.id.llOperationButtons);
		mOperationButtons.setVisibility(View.GONE);
		AnimationHelpers.fadeOut(mOperationButtons, 0, 0, false);

		hookupMediaOperationButtons();

		// If this is "edit" and not "add" the edited story is sent in the
		// intent. Get it!
		if (getIntent().hasExtra("story"))
		{
			long storyId = getIntent().getLongExtra("story", 0);

			for (Item story : App.getInstance().socialReporter.getDrafts())
			{
				if (story.getDatabaseId() == storyId)
				{
					mStory = story;
					break;
				}
			}
		}

		// Hacking this in...
		// http://developer.android.com/training/sharing/receive.html
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		if (Intent.ACTION_SEND.equals(action) && type != null)
		{
			if ("text/plain".equals(type))
			{
				String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
				mEditContent.setText(sharedText);
			}
			else if (type.startsWith("image/"))
			{
				Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);

				// addMediaItem(imageUri, type, -1);
			}
			else if (type.startsWith("video/"))
			{
				Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
				// addMediaItem(imageUri, type, -1);
			}
		}

		// We need to react on the soft keyboard being shown, to make sure that
		// the input fields are
		// shown correctly. This is taken from:
		// http://stackoverflow.com/questions/2150078/how-to-check-visibility-of-software-keyboard-in-android/4737265#4737265
		//
		final View activityRootView = findViewById(R.id.llRoot);
		activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener()
		{
			@Override
			public void onGlobalLayout()
			{
				Rect r = new Rect();
				// r will be populated with the coordinates of your view
				// that
				// area still visible.
				activityRootView.getWindowVisibleDisplayFrame(r);

				int heightDiff = activityRootView.getRootView().getHeight() - r.height();
				if (heightDiff > 100)
				{ // if more than 100 pixels, its
					// probably a keyboard...
					final ScrollView sv = (ScrollView) findViewById(R.id.sv0);
					if (sv != null)
					{
						sv.post(new Runnable()
						{
							@Override
							public void run()
							{
								sv.scrollTo(0, sv.getBottom());
							}
						});
					}
				}
			}
		});

		mViewSignIn = (PostSignInView) findViewById(R.id.signIn);
		mViewSignIn.setActionListener(this);

		mViewCreateAccount = (CreateAccountView) findViewById(R.id.createAccount);
		mViewCreateAccount.setActionListener(this);

		mEditTags.addTextChangedListener(new TagTextWatcher());

		showHideCreateAccount(false);

		// Sat focus change listener so we can auto save draft on lost focus
		// event
		mEditTitle.setOnFocusChangeListener(this);
		mEditContent.setOnFocusChangeListener(this);
		mEditTags.setOnFocusChangeListener(this);

		populateFromStory();
		updateMediaControls();
	}

	private void hookupMediaOperationButtons()
	{
		mProgressIcon = findViewById(R.id.ivProgressIcon);
		mProgressIcon.setVisibility(View.GONE);
		
		mBtnMediaAdd = findViewById(R.id.btnMediaAdd);
		mBtnMediaAdd.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				createMediaChooser(-1);
			}
		});

		mBtnMediaAddMore = findViewById(R.id.btnMediaAddMore);
		mBtnMediaAddMore.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				createMediaChooser(-1);
			}
		});

		mBtnMediaReplace = findViewById(R.id.btnMediaReplace);
		mBtnMediaReplace.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				int currentIndex = mMediaView.getCurrentItemIndex();
				createMediaChooser(currentIndex);
			}
		});

		mBtnMediaView = findViewById(R.id.btnMediaView);
		mBtnMediaView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				int currentIndex = mMediaView.getCurrentItemIndex();
				if (mStory != null && currentIndex < mStory.getNumberOfMediaContent())
				{
					MediaContent mediaContent = mStory.getMediaContent().get(currentIndex);
					Bundle mediaData = new Bundle();
					mediaData.putSerializable("media", mediaContent);
					UICallbacks.handleCommand(v.getContext(), R.integer.command_view_media, mediaData);
				}
			}
		});

		mBtnMediaDelete = findViewById(R.id.btnMediaDelete);
		mBtnMediaDelete.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				int currentIndex = mMediaView.getCurrentItemIndex();
				if (mStory != null && currentIndex < mStory.getNumberOfMediaContent())
				{
					mStory.getMediaContent().remove(currentIndex);
					onMediaChanged();
				}
			}
		});
	}

	private java.io.File getAlbumDir()
	{
		return new java.io.File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), App.getInstance().getString(R.string.app_name));
	}

	private void createImageFile(boolean isVideo) throws IOException
	{
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = timeStamp + "_";
		java.io.File dir = getAlbumDir();
		dir.mkdir();
		java.io.File image = File.createTempFile(imageFileName, isVideo ? ".mp4" : ".jpg", dir);
		mCurrentPhotoFile = image;
	}

	private void deleteImageFile()
	{
		if (mCurrentPhotoFile != null)
		{
			mCurrentPhotoFile.delete();
			mCurrentPhotoFile = null;
		}
	}

	private void copyFileFromFStoAppFS(java.io.InputStream in, java.io.File src, info.guardianproject.iocipher.File dst) throws IOException
	{
		if (in == null)
			in = new java.io.FileInputStream(src);
		OutputStream out = new info.guardianproject.iocipher.FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0)
		{
			out.write(buf, 0, len);
			if (LOGGING)
				Log.v(LOGTAG, "Writing:"+len);
		}
		in.close();
		out.close();

		if (LOGGING)
			Log.v(LOGTAG, "copyFileFromFStoAppFS Copied from " + ((src == null) ? "stream" : src.toString()) + " to " + dst.toString());
	}

	private void updateMediaControls()
	{
		// Show or hide the add media button
		if (mStory == null || mStory.getNumberOfMediaContent() == 0)
		{
			mMediaView.setVisibility(View.INVISIBLE);
			mBtnMediaAdd.setVisibility(View.VISIBLE);
			mOperationButtons.setVisibility(View.GONE);
		}
		else
		{
			mMediaView.setVisibility(View.VISIBLE);
			mBtnMediaAdd.setVisibility(View.GONE);
			mOperationButtons.setVisibility(View.GONE);
		}
	}

	private void populateFromStory()
	{
		if (mStory == null)
			return;
		mEditTitle.setText(mStory.getTitle());
		mEditContent.setText(mStory.getCleanMainContent());

		ArrayList<String> tags = mStory.getTags();
		if (tags != null)
		{
			StringBuilder sb = new StringBuilder();
			for (String tag : tags)
			{
				if (sb.length() > 0)
					sb.append(" ");
				sb.append(tag);
			}
			mEditTags.setText(sb.toString());
		}

		MediaViewCollection collection = new MediaViewCollection(mMediaView.getContext(), mStory);
		collection.load(true, false);
		mMediaView.setMediaCollection(collection, false, false);
	}

	private ArrayList<String> getTagsFromInput()
	{
		ArrayList<String> ret = null;

		String tagsString = mEditTags.getText().toString();
		if (tagsString != null)
		{
			tagsString = tagsString.replace("#", ""); // remove # from the
														// string
			if (tagsString.length() > 0)
				ret = new ArrayList<String>(Arrays.asList(tagsString.split("\\s+", 0)));
		}
		return ret;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		if (mEditTitle.getText().length() > 0)
			outState.putString("title", mEditTitle.getText().toString());
		if (mEditContent.getText().length() > 0)
			outState.putString("content", mEditContent.getText().toString());
		if (mEditTags.getText().length() > 0)
			outState.putString("tags", mEditTags.getText().toString());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);

		if (savedInstanceState.containsKey("title"))
			mEditTitle.setText(savedInstanceState.getString("title"));
		if (savedInstanceState.containsKey("content"))
			mEditContent.setText(savedInstanceState.getString("content"));
		if (savedInstanceState.containsKey("tags"))
			mEditTags.setText(savedInstanceState.getString("tags"));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		boolean ret = super.onCreateOptionsMenu(menu);

		MenuItem menuPost = menu.findItem(R.id.menu_post);
		if (menuPost != null)
		{
			MenuItemCompat.setActionProvider(menuPost, new ActionProvider(this)
			{
				@Override
				public View onCreateActionView()
				{
					LayoutInflater inflater = LayoutInflater.from(AddPostActivity.this);
					View view = inflater.inflate(R.layout.actionbar_green_button, null);
					view.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
					TextView tv = (TextView) view.findViewById(R.id.tvItem);
					tv.setText(R.string.add_post_menu_post);
					view.setOnClickListener(new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							if (!checkValidPost())
							{
								showNoValidDataWarning();
								return;
							}

							saveDraft(false);
							loadingDialog = ProgressDialog.show(AddPostActivity.this, "", "Posting. Please wait...", true, true);

							XMLRPCPublisherCallback publisherCallback = new XMLRPCPublisherCallback()
							{

								@Override
								public void itemPublished(int itemId) {
									if (loadingDialog.isShowing())
									{
										loadingDialog.dismiss();
										quitBackToList(1); // "outbox"
									}
								}

								@Override
								public void publishingFailed(int reason) {
									loadingDialog.dismiss();

									// Should add in reasons
									if (reason == XMLRPCPublisher.FAILURE_REASON_NO_PRIVACY_PROXY) {
										Toast.makeText(AddPostActivity.this, "Posting Failed, Tor or Psiphon Required for Posting", Toast.LENGTH_SHORT).show();
									} else if (reason == XMLRPCPublisher.FAILURE_REASON_NO_CONNECTION) {
										Toast.makeText(AddPostActivity.this, "Posting Failed, No Network Connection", Toast.LENGTH_SHORT).show();
									} else {
										Toast.makeText(AddPostActivity.this, "Posting Failed, Failure Unknown", Toast.LENGTH_SHORT).show();
									}
								}

							};
							App.getInstance().socialReporter.publish(mStory, publisherCallback);
						}
					});
					return view;
				}

				@Override
				public boolean hasSubMenu()
				{
					return false;
				}
			});
		}

		return ret;
	}

	private boolean saveDraftOrAskForDeletion()
	{
		// Does it contain data at the moment?
		if (isEmpty() && (mStory == null || mStory.getNumberOfMediaContent() == 0))
		{
			// No. If we have saved a draft already, ask if user wants to
			// delete it
			if (mStory != null)
			{
				Builder alert = new AlertDialog.Builder(this).setPositiveButton(R.string.add_post_draft_delete, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
						App.getInstance().socialReporter.deleteDraft(mStory);
						quitBackToList(-1); // wherever we were
											// before
					}
				}).setNegativeButton(R.string.add_post_draft_save, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if (saveDraft(false))
							Toast.makeText(AddPostActivity.this, R.string.add_post_draft_saved, Toast.LENGTH_SHORT).show();
						dialog.dismiss();
						quitBackToList(2); // drafts
					}
				}).setMessage(R.string.add_post_draft_delete_empty);
				alert.show();
				return false;
			}
		}

		if (saveDraft(false))
			Toast.makeText(this, R.string.add_post_draft_saved, Toast.LENGTH_SHORT).show();

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			if (this.saveDraftOrAskForDeletion())
				quitBackToList(2); // drafts
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onUnlockedActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent)
	{
		super.onUnlockedActivityResult(requestCode, resultCode, imageReturnedIntent);

		switch (requestCode)
		{
		case REQ_CODE_PICK_IMAGE:
			if (resultCode == RESULT_OK)
			{
				try
				{
					String defaultType = "image/jpeg";
					if (mStartedIntent.getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE))
					{
						this.addMediaItem(null, mCurrentPhotoFile, null, defaultType, mReplaceThisIndex);
					}
					else if (mStartedIntent.getAction().equals(MediaStore.ACTION_VIDEO_CAPTURE))
					{
						defaultType = "video/mp4";
						this.addMediaItem(null, mCurrentPhotoFile, null, defaultType, mReplaceThisIndex);
					}
					else
					{
						java.io.File mediaItemFile = null;
						java.io.InputStream mediaItemStream = null;

						String[] projections = new String[] { android.provider.MediaStore.Images.ImageColumns.DATA,
								android.provider.MediaStore.Images.ImageColumns.MIME_TYPE };
						Cursor cursor = getContentResolver().query(imageReturnedIntent.getData(), projections, null, null, null);
						if (cursor != null)
						{
							if (cursor.moveToFirst())
							{
								int idxData = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.ImageColumns.DATA);
								int idxMime = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.ImageColumns.MIME_TYPE);

								String path = cursor.getString(idxData);
								if (path != null)
								{
									mediaItemFile = new java.io.File(path);
								}
								String mediaType = cursor.getString(idxMime);
								if (mediaType != null)
								{
									defaultType = mediaType;
								}
							}
							cursor.close();
						}
						if (mediaItemFile == null)
						{
							Uri uriMedia = imageReturnedIntent.getData();
							try
							{
								mediaItemStream = getContentResolver().openInputStream(uriMedia);
							}
							catch (Exception ex)
							{
								mediaItemStream = null;
							}
						}

						this.addMediaItem(mediaItemStream, mediaItemFile, imageReturnedIntent.getData().toString(), defaultType, mReplaceThisIndex);
					}
				}
				catch (Exception ex)
				{
					if (LOGGING)
						Log.e(LOGTAG, "Failed to add image/video: " + ex.toString());
				}
			}
			else
			{
				// Delete temp file, if we created one
				deleteImageFile();
			}
		}
	}

	/**
	 * Add or replace a piece of media in the current draft.
	 * 
	 * @param mediaItem
	 *            Uri to the new media to add
	 * @param defaultType
	 *            If type can't be decided automatically, use this as a default
	 *            type
	 * @param replaceIndex
	 *            Optional index to replace (-1 for normal add)
	 */
	// private void addMediaItem(Uri mediaItem, String defaultType, int
	// replaceIndex)
	// We want files here so we can deal with them all the same
	private void addMediaItem(java.io.InputStream mediaItemStream, java.io.File mediaItemFile, String mediaItemUrl, String mediaType, int replaceIndex)
	{
		if (mStory == null)
			saveDraft(true);

		MediaContent newMediaContent = new MediaContent(mStory.getDatabaseId(), "", mediaType);
		// Let's get a record with an id
		if (replaceIndex == -1)
		{
			mStory.addMediaContent(newMediaContent);
		}
		else
		{
			ArrayList<MediaContent> rgMedia = mStory.getMediaContent();
			rgMedia.remove(replaceIndex);
			rgMedia.add(replaceIndex, newMediaContent);
		}
		// This should give us a database id if one doesn't exist already
		saveDraft(false);

		mProgressIcon.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate));
		mProgressIcon.setVisibility(View.VISIBLE);
		mBtnMediaAdd.setVisibility(View.GONE);
		mIsAddingMedia = true;
	
		ThreadedTask<Void, Void, Void> addMediaTask = new ThreadedTask<Void, Void, Void>() 
		{	
			private InputStream mediaItemStream;
			private java.io.File mediaItemFile;
			private String mediaItemUrl;
			private MediaContent mediaContent;

			public ThreadedTask<Void, Void, Void> init(java.io.InputStream mediaItemStream, java.io.File mediaItemFile, String mediaItemUrl, MediaContent mediaContent)
			{
				this.mediaItemStream = mediaItemStream;
				this.mediaItemFile = mediaItemFile;
				this.mediaItemUrl = mediaItemUrl;
				this.mediaContent = mediaContent;
				return this;
			}
			
			@Override
			protected Void doInBackground(Void... values) 
			{
				if (mediaItemStream != null || mediaItemFile != null)
				{
					// Now we can copy the file
					File outputFile;
					outputFile = new File(App.getInstance().socialReader.getFileSystemDir(), SocialReader.MEDIA_CONTENT_FILE_PREFIX + mediaContent.getDatabaseId());
					
					if (LOGGING)
						Log.v(LOGTAG, "Local App Storage File: " + outputFile);

					// First copy file to encrypted storage
					try
					{
						copyFileFromFStoAppFS(mediaItemStream, mediaItemFile, outputFile);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}

					// Update record in media content
					mediaContent.setUrl("file://" + outputFile.getAbsolutePath());
				}
				else
				{
					mediaContent.setUrl(mediaItemUrl);
				}
				if (LOGGING)
					Log.v(LOGTAG, "Set Url to: " + mediaContent.getUrl());
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				mIsAddingMedia = false;

				mProgressIcon.clearAnimation();
				mProgressIcon.setVisibility(View.GONE);

				// If this was a temp capture file, delete it
				deleteImageFile();
				onMediaChanged();
			}
		}.init(mediaItemStream, mediaItemFile, mediaItemUrl, newMediaContent);
		addMediaTask.execute((Void)null);
	}

	private void onMediaChanged()
	{
		// Save our changes
		saveDraft(false);

		// Update the media view to show new media as well
		MediaViewCollection collection = new MediaViewCollection(mMediaView.getContext(), mStory);
		collection.load(true, false);
		mMediaView.setMediaCollection(collection, false, false);
		updateMediaControls();
	}

	private boolean isEmpty()
	{
		if (TextUtils.isEmpty(mEditTitle.getText()) && TextUtils.isEmpty(mEditContent.getText()))
		{
			ArrayList<String> tags = getTagsFromInput();
			if (tags == null || tags.size() == 0)
				return true;
		}
		return false;
	}

	private boolean saveDraft(boolean forceCreate)
	{
		if (LOGGING) 
			Log.v(LOGTAG, "Saving draft");
		
		if (mStory == null)
		{
			// If nothing had been changed yet, no need to save!
			if (!forceCreate && TextUtils.isEmpty(mEditTitle.getText()) && TextUtils.isEmpty(mEditContent.getText()) && TextUtils.isEmpty(mEditTags.getText()))
				return false;

			mStory = App.getInstance().socialReporter.createDraft(mEditTitle.getText().toString(), mEditContent.getText().toString(), getTagsFromInput(), null);
			getIntent().putExtra("story", mStory.getDatabaseId());
		}
		else
		{
			mStory.setTitle(mEditTitle.getText().toString());
			mStory.setDescription(mEditContent.getText().toString());
			mStory.getTags().clear();
			ArrayList<String> tags = getTagsFromInput();
			if (tags != null && tags.size() > 0)
				mStory.getTags().addAll(tags);
		}
		App.getInstance().socialReporter.saveDraft(mStory);

		if (LOGGING)
			Log.v(LOGTAG, "SaveDraft: Story Database Id: " + mStory.getDatabaseId());

		return true;
	}

	private boolean checkValidPost()
	{
		if (TextUtils.isEmpty(mEditTitle.getText()))
			return false;
		return true;
	}

	private void showNoValidDataWarning()
	{
		Builder alert = new AlertDialog.Builder(this).setTitle(R.string.add_post_not_valid_data_warning_title)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				}).setMessage(R.string.add_post_not_valid_data_warning_content);
		alert.show();
	}

	private class TagTextWatcher implements TextWatcher
	{
		private boolean mRemoved;
		private boolean mChanging;
		private final int mHashColor;

		public TagTextWatcher()
		{
			mHashColor = getResources().getColor(R.color.grey_light_medium);
		}

		@Override
		public void afterTextChanged(Editable s)
		{
			if (!mChanging)
			{
				if (mRemoved)
				{
					if (s.length() > 0)
					{
						int remove = 0;
						int idx = s.length() - 1;
						while (Character.isWhitespace(s.charAt(idx)))
						{
							remove++;
							idx--;
						}
						if (remove > 0)
						{
							mChanging = true;
							s.delete(s.length() - remove, s.length());
							mChanging = false;
						}
					}
				}
				else
				{
					if (s.length() == 0 || Character.isWhitespace(s.charAt(s.length() - 1)))
					{
						mChanging = true;
						s.append("#");
						mChanging = false;
					}
					else if (s.length() > 0 && s.charAt(0) != '#')
					{
						mChanging = true;
						s.insert(0, "#");
						mChanging = false;
					}
				}

				mChanging = true;

				// Simply doing s.clearSpans does not work, see
				// http://stackoverflow.com/questions/9403057/android-edittext-clearing-spans
				ForegroundColorSpan[] toRemoveSpans = s.getSpans(0, s.length(), ForegroundColorSpan.class);
				for (int i = 0; i < toRemoveSpans.length; i++)
					s.removeSpan(toRemoveSpans[i]);

				// Style the hashes in a more discrete grey color
				for (int i = s.length() - 1; i >= 0; i--)
				{
					if (s.charAt(i) == '#')
					{
						s.setSpan(new ForegroundColorSpan(mHashColor), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

						// Make sure a space exists before hashes
						if (i > 0 && !Character.isWhitespace(s.charAt(i - 1)))
						{
							s.insert(i, " ");
						}
					}
				}
				mChanging = false;

			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after)
		{

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{
			if (count < before)
				mRemoved = true;
			else
				mRemoved = false;
		}
	}

	private void quitBackToList(int go_to_tab)
	{
		Bundle commandParameters = null;
		if (go_to_tab != -1)
		{
			commandParameters = new Bundle();
			commandParameters.putInt("go_to_tab", go_to_tab);
		}
		UICallbacks.handleCommand(this, R.integer.command_posts_list, commandParameters);
		overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
		finish();
	}

	@Override
	public void onBackPressed()
	{
		if (saveDraftOrAskForDeletion())
			quitBackToList(2); // drafts
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus)
	{
		saveDraft(false);
	}

	private class HandlerIntent
	{
		public final Intent intent;
		public final ResolveInfo resolveInfo;

		public HandlerIntent(Intent intent, ResolveInfo resolveInfo)
		{
			this.intent = intent;
			this.resolveInfo = resolveInfo;
		}
	}

	private class HandlerIntentListAdapter extends ArrayAdapter<HandlerIntent>
	{
		public HandlerIntentListAdapter(Context context, HandlerIntent[] intents)
		{
			super(context, android.R.layout.select_dialog_item, android.R.id.text1, intents);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			// User super class to create the View
			View v = super.getView(position, convertView, parent);
			TextView tv = (TextView) v.findViewById(android.R.id.text1);

			HandlerIntent handlerIntent = getItem(position);
			ResolveInfo info = handlerIntent.resolveInfo;
			PackageManager pm = getContext().getPackageManager();
			tv.setText(info.loadLabel(pm));

			Drawable icon = info.loadIcon(pm);
			int iconSize = UIHelpers.dpToPx(32, getContext());
			icon.setBounds(0, 0, iconSize, iconSize);

			// Put the image on the TextView
			tv.setCompoundDrawables(icon, null, null, null);
			tv.setCompoundDrawablePadding(UIHelpers.dpToPx(10, getContext()));

			return v;
		}
	};

	private void getHandlersForIntent(Intent intent, ArrayList<HandlerIntent> rgIntents)
	{
		PackageManager pm = getPackageManager();
		List<ResolveInfo> resInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

		for (ResolveInfo resInfo : resInfos)
		{
			rgIntents.add(new HandlerIntent(intent, resInfo));
		}
	}

	private void createMediaChooser(int replaceThisIndex)
	{
		mReplaceThisIndex = replaceThisIndex; // Remember which index to
												// replace, if any (set to -1
												// for "add")

		Builder alert = new AlertDialog.Builder(this);

		LayoutInflater inflater = LayoutInflater.from(this);
		View contentView = inflater.inflate(R.layout.add_post_media_chooser, null);

		TabHost tabHost = (TabHost) contentView.findViewById(android.R.id.tabhost);
		tabHost.setup();

		TabSpec spec = tabHost.newTabSpec("photo");
		View tabView = inflater.inflate(R.layout.light_tab_item, null);
		((TextView) tabView.findViewById(R.id.tvItem)).setText(getString(R.string.add_post_media_chooser_photo));
		spec.setIndicator(tabView);
		spec.setContent(R.id.lvPhoto);
		tabHost.addTab(spec);

		ListView lv = (ListView) contentView.findViewById(R.id.lvPhoto);

		// Populate photo intents
		final ArrayList<HandlerIntent> rgIntentsPhoto = new ArrayList<HandlerIntent>();
		Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		getHandlersForIntent(cameraIntent, rgIntentsPhoto);
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		getHandlersForIntent(intent, rgIntentsPhoto);
		ListAdapter adapterPhoto = new HandlerIntentListAdapter(this, rgIntentsPhoto.toArray(new HandlerIntent[rgIntentsPhoto.size()]));
		lv.setAdapter(adapterPhoto);
		lv.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				startResolvedIntent((HandlerIntent) parent.getAdapter().getItem(position));
			}
		});

		spec = tabHost.newTabSpec("video");
		tabView = inflater.inflate(R.layout.light_tab_item, null);
		((TextView) tabView.findViewById(R.id.tvItem)).setText(getString(R.string.add_post_media_chooser_video));
		spec.setIndicator(tabView);
		spec.setContent(R.id.lvVideo);
		tabHost.addTab(spec);

		// Populate video intents
		lv = (ListView) contentView.findViewById(R.id.lvVideo);
		lv.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				startResolvedIntent((HandlerIntent) parent.getAdapter().getItem(position));
			}
		});

		final ArrayList<HandlerIntent> rgIntentsVideo = new ArrayList<HandlerIntent>();
		Intent cameraIntentVideo = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		getHandlersForIntent(cameraIntentVideo, rgIntentsVideo);
		intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("video/*");
		getHandlersForIntent(intent, rgIntentsVideo);
		ListAdapter adapterVideo = new HandlerIntentListAdapter(this, rgIntentsVideo.toArray(new HandlerIntent[rgIntentsVideo.size()]));
		lv.setAdapter(adapterVideo);

		alert.setView(contentView);

		mMediaChooserDialog = alert.show();
	}

	protected void startResolvedIntent(HandlerIntent info)
	{
		try
		{
			if (MediaStore.ACTION_IMAGE_CAPTURE.equals(info.intent.getAction()) || MediaStore.ACTION_VIDEO_CAPTURE.equals(info.intent.getAction()))
			{
				boolean bIsVideo = MediaStore.ACTION_VIDEO_CAPTURE.equals(info.intent.getAction());
				createImageFile(bIsVideo);
				Uri uriSavedImage = Uri.fromFile(mCurrentPhotoFile.getAbsoluteFile());
				info.intent.putExtra(MediaStore.EXTRA_OUTPUT, uriSavedImage);
			}
			info.intent.setClassName(info.resolveInfo.activityInfo.packageName, info.resolveInfo.activityInfo.name);
			mStartedIntent = info.intent;
			startActivityForResult(info.intent, REQ_CODE_PICK_IMAGE);
			mMediaChooserDialog.dismiss();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			deleteImageFile();
		}
	}

	private void showHideCreateAccount(boolean animate)
	{
		if (App.getSettings().acceptedPostPermission())
		{
			mViewCreateAccount.setVisibility(View.GONE);
			if (animate)
				AnimationHelpers.fadeOut(mViewSignIn, 500, 0, false, this);
			else
				mViewSignIn.setVisibility(View.GONE);
		}
		else if (App.getInstance().socialReporter.getAuthorName() != null)
		{
			if (animate)
			{
				AnimationHelpers.fadeOut(mViewCreateAccount, 500, 0, false, this);
				AnimationHelpers.fadeIn(mViewSignIn, 500, 0, false, this);
			}
			else
			{
				mViewSignIn.setVisibility(View.VISIBLE);
				mViewCreateAccount.setVisibility(View.GONE);
			}
		}
		else
		{
			mViewCreateAccount.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onCreateIdentity(String authorName)
	{
		App.getInstance().socialReporter.createAuthorName(authorName);
		showHideCreateAccount(true);
	}

	@Override
	public void onAgreed()
	{
		App.getSettings().setAcceptedPostPermission(true);
		showHideCreateAccount(true);
	}

	@Override
	protected void onWipe()
	{
		super.onWipe();
		quitBackToList(0); // published
	}

	@Override
	public void onFadeInStarted(View view)
	{
		if (view == mViewSignIn)
			view.setVisibility(View.VISIBLE);
	}

	@Override
	public void onFadeInEnded(View view)
	{
	}

	@Override
	public void onFadeOutStarted(View view)
	{
	}

	@Override
	public void onFadeOutEnded(View view)
	{
		view.setVisibility(View.GONE);
		// To avoid old device bug, see
		// http://stackoverflow.com/questions/4728908/android-view-with-view-gone-still-receives-ontouch-and-onclick
		view.clearAnimation();
	}

}
