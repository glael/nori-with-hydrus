package pe.moe.nori;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.util.LruCache;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import pe.moe.nori.api.BooruClient;
import pe.moe.nori.api.Image;
import pe.moe.nori.api.SearchResult;
import pe.moe.nori.providers.ServiceSettingsProvider;

public class ImageViewerActivity extends SherlockActivity implements ViewPager.OnPageChangeListener {
  /** Current SearchResult */
  private SearchResult mSearchResult;
  /** HTTP request queue */
  private RequestQueue mRequestQueue;
  /** API client */
  private BooruClient mBooruClient;
  /** Bitmap LRU cache */
  private LruCache<String, Bitmap> mBitmapLruCache = new LruCache<String, Bitmap>(4096) {
    @Override
    protected int sizeOf(String key, Bitmap value) {
      // Convert size to kilobytes.
      return (int) ((long) value.getRowBytes() * (long) value.getHeight() / 1024);
    }
  };
  /** Volley image cache */
  private ImageLoader.ImageCache mImageCache = new ImageLoader.ImageCache() {
    @Override
    public Bitmap getBitmap(String url) {
      return mBitmapLruCache.get(url);
    }

    @Override
    public void putBitmap(String url, Bitmap bitmap) {
      mBitmapLruCache.put(url, bitmap);
    }
  };
  /** Volley image lazyloader */
  private ImageLoader mImageLoader;
  private ViewPager mViewPager;

  private void downloadCurrentItem() {
    final DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
    final Image image = mSearchResult.images.get(mViewPager.getCurrentItem());
    final String fileName = image.fileUrl.substring(image.fileUrl.lastIndexOf("/") + 1);

    // Create download directory if it doesn't exist.
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdirs();

    // Create and enqueue request.
    final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(image.fileUrl))
        .setTitle(fileName)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        .setVisibleInDownloadsUi(false);
    request.allowScanningByMediaScanner();
    downloadManager.enqueue(request);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Get SearchResult and API settings from Intent.
    mSearchResult = getIntent().getParcelableExtra("pe.moe.nori.api.SearchResult");
    mRequestQueue = Volley.newRequestQueue(this);
    mBooruClient = ServiceSettingsProvider.ServiceSettings.createClient(mRequestQueue,
        getIntent().<ServiceSettingsProvider.ServiceSettings>getParcelableExtra("pe.moe.nori.api.Settings"));
    mImageLoader = new ImageLoader(mRequestQueue, mImageCache);

    // Inflate content view.
    setContentView(R.layout.activity_imageviewer);
    mViewPager = (ViewPager) findViewById(R.id.pager);
    mViewPager.setAdapter(new SearchResultPagerAdapter(this, mSearchResult));
    mViewPager.setOffscreenPageLimit(5);
    mViewPager.setOnPageChangeListener(this);

    // Load current position from Intent if not restored from instance state.
    if (savedInstanceState == null)
      mViewPager.setCurrentItem(getIntent().getIntExtra("pe.moe.nori.api.SearchResult.position", 0));

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getSupportMenuInflater().inflate(R.menu.imageviewer, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_download:
        downloadCurrentItem();
        return true;
      default:
        return false;
    }
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    // Trim bitmap cache to reduce memory usage.
    mBitmapLruCache.trimToSize(32);
  }

  @Override
  public void onPageScrolled(int i, float v, int i2) {

  }

  @Override
  public void onPageSelected(int pos) {
    // Set activity title.
    final StringBuilder stringBuilder = new StringBuilder();
    final Image image = mSearchResult.images.get(pos);
    stringBuilder.append(image.id);
    stringBuilder.append(": ");

    for (int i = 0; i < image.generalTags.length; i++) {
      stringBuilder.append(image.generalTags[i] + " ");
    }
    if (stringBuilder.length() > 25) {
      stringBuilder.setLength(24);
      stringBuilder.append("…");
    }

    setTitle(stringBuilder.toString());
  }

  @Override
  public void onPageScrollStateChanged(int i) {

  }

  /** Adapter for the {@link ViewPager} used for flipping through the images. */
  private class SearchResultPagerAdapter extends PagerAdapter {
    private final Context mContext;
    private final SearchResult mSearchResult;

    public SearchResultPagerAdapter(Context context, SearchResult searchResult) {
      // Set search result and context.
      this.mSearchResult = searchResult;
      this.mContext = context;
    }

    @Override
    public NetworkImageView instantiateItem(ViewGroup container, int position) {
      // Create ImageView.
      final NetworkImageView networkImageView = new NetworkImageView(mContext);
      networkImageView.setLayoutParams(new AbsListView.LayoutParams(ViewPager.LayoutParams.MATCH_PARENT, ViewPager.LayoutParams.MATCH_PARENT));
      networkImageView.setImageUrl(mSearchResult.images.get(position).fileUrl, mImageLoader);

      // Add ImageView to the View container.
      container.addView(networkImageView);
      return networkImageView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
      // Remove View from the container.
      container.removeView((View) object);
    }

    @Override
    public int getCount() {
      return mSearchResult.images.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
      // Checks if View corresponds to the given object returned from #instantiateItem()
      // Just check if view == o, since #instantiateItem() returns the view anyways.
      return view == o;
    }
  }
}